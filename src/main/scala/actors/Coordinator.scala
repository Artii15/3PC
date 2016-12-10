package actors

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props}
import config.CoordinatorConfig
import messages._

import scala.concurrent.duration._

class Coordinator(coordinatorConfig: CoordinatorConfig) extends Actor {
  import actors.states.CoordinatorState._

  private var notAgreedWorkersCount = 0
  private var pendingAck = 0
  private var transactionRequester: Option[ActorRef] = None
  private var currentTransactionId: Option[UUID] = None

  override def preStart(): Unit = {
    val cohortLocations = Stream.continually(coordinatorConfig.getCohortLocations).flatten
    cohortLocations.take(coordinatorConfig.getCohortSize)
      .foreach(actorLocation => context.system.actorOf(Props[Worker], actorLocation))
  }

  override def receive: Receive = {
    case TransactionBeginRequest(requester) => beginTransaction(requester)
  }

  private def beginTransaction(requester: ActorRef): Unit = {
    val transactionUUID = UUID.randomUUID()
    currentTransactionId = Some(transactionUUID)
    transactionRequester = Some(requester)

    requester ! TransactionBeginAck

    val timeout = makeTimeoutForState(INITIALIZING)
    context.system.scheduler.scheduleOnce(coordinatorConfig.getTransactionOperationsTimeout seconds, self, timeout)
    context become initializer
  }

  private def initializer: Receive = {
    case operations: TransactionOperations => executeOperations(operations)
    case TransactionCommitRequest => initializeCommit()
    case Failure
         | CoordinatorTimeout(transactionId, INITIALIZING) if equalsCurrentTransactionId(transactionId) => abort()
  }

  private def executeOperations(operations: TransactionOperations): Unit = context.children.foreach(_ ! operations)

  private def initializeCommit(): Unit = {
    context.children.foreach(_ ! TransactionCommitRequest)
    notAgreedWorkersCount = coordinatorConfig.getCohortSize

    val timeout = makeTimeoutForState(WAITING_AGREE)
    context.system.scheduler.scheduleOnce(coordinatorConfig.getWaitingAgreeTimeout seconds, self, timeout)
    context become tryingToWrite
  }

  private def tryingToWrite: Receive = {
    case CommitAgree => receiveAgree()
    case Failure
         | CoordinatorTimeout(transactionId, WAITING_AGREE) if equalsCurrentTransactionId(transactionId)  => abort()
  }

  private def equalsCurrentTransactionId(id: Option[UUID]): Boolean = for {
    currentId <- currentTransactionId
    otherTransactionId <- id
    areIDsEqual <- currentId == otherTransactionId
  } yield areIDsEqual

  private def receiveAgree(): Unit = {
    notAgreedWorkersCount -= 1
    if(notAgreedWorkersCount == 0) {
      context.children.foreach(_ ! PrepareToCommit)
      pendingAck = coordinatorConfig.getCohortSize
      context become preparingToCommit
    }
  }

  private def preparingToCommit: Receive = {
    case CommitAck => receiveCommitAck()
  }

  private def receiveCommitAck(): Unit = {
    pendingAck -= 1
    if(pendingAck == 0) {
      transactionRequester.foreach(_ ! CommitConfirmation())
      finishCurrentTransaction()
    }
  }

  private def finishCurrentTransaction(): Unit = {
    transactionRequester = None
    currentTransactionId = None
    context become receive
  }

  private def abort(): Unit = {
    context.children.foreach(_ ! Abort)
    finishCurrentTransaction()
  }

  private def makeTimeoutForState(coordinatorState: CoordinatorState) =
    CoordinatorTimeout(currentTransactionId, coordinatorState)
}
