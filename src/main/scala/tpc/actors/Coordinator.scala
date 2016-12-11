package tpc.actors

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props}
import tpc.{ConcreteID, EmptyID, TransactionId}
import tpc.config.CoordinatorConfig
import tpc.messages._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class Coordinator(coordinatorConfig: CoordinatorConfig) extends Actor {
  import tpc.actors.states.CoordinatorState._

  private var notAgreedWorkersCount = 0
  private var pendingAck = 0
  private var transactionRequester: Option[ActorRef] = None
  private var currentTransactionId: TransactionId = EmptyID

  override def preStart(): Unit = {
    val cohortLocations = Stream.continually(coordinatorConfig.getCohortLocations).flatten
    cohortLocations.take(coordinatorConfig.getCohortSize)
      .foreach(actorLocation => context.system.actorOf(Props[Worker], actorLocation))
  }

  override def receive: Receive = {
    case TransactionBeginRequest(requester) => beginTransaction(requester)
  }

  private def beginTransaction(requester: ActorRef): Unit = {
    currentTransactionId = ConcreteID(UUID.randomUUID())
    transactionRequester = Some(requester)

    context.children.foreach(_ ! TransactionBeginOrder(currentTransactionId))
    requester ! TransactionBeginAck

    val timeout = makeTimeoutForState(INITIALIZING)
    context.system.scheduler.scheduleOnce(coordinatorConfig.getTransactionOperationsTimeout seconds, self, timeout)
    context become initializer
  }

  private def initializer: Receive = {
    case operations: TransactionOperations => executeOperations(operations)
    case TransactionCommitRequest => initializeCommit()
    case Failure => abort()
    case CoordinatorTimeout(transactionId, INITIALIZING) if transactionId == currentTransactionId => abort()
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
    case Failure => abort()
    case CoordinatorTimeout(transactionId, WAITING_AGREE) if transactionId == currentTransactionId  => abort()
  }

  private def receiveAgree(): Unit = {
    notAgreedWorkersCount -= 1
    if(notAgreedWorkersCount == 0) {
      context.children.foreach(_ ! PrepareToCommit)
      pendingAck = coordinatorConfig.getCohortSize

      val timeout = makeTimeoutForState(WAITING_ACK)
      context.system.scheduler.scheduleOnce(coordinatorConfig.getWaitingAckTimeout seconds, self, timeout)

      context become preparingToCommit
    }
  }

  private def preparingToCommit: Receive = {
    case CommitAck => receiveCommitAck()
    case Failure => doCommit()
    case CoordinatorTimeout(transactionId, WAITING_ACK) if transactionId == currentTransactionId => abort()
  }

  private def receiveCommitAck(): Unit = {
    pendingAck -= 1
    if(pendingAck == 0) doCommit()
  }

  private def doCommit(): Unit = {
    context.children.foreach(_ ! CommitConfirmation)
    transactionRequester.foreach(_ ! CommitConfirmation)
    cleanUpAfterTransaction()
  }

  private def abort(): Unit = {
    context.children.foreach(_ ! Abort)
    transactionRequester.foreach(_! Abort)
    cleanUpAfterTransaction()
  }

  private def cleanUpAfterTransaction(): Unit = {
    transactionRequester = None
    currentTransactionId = EmptyID
    context become receive
  }

  private def makeTimeoutForState(coordinatorState: CoordinatorState) = CoordinatorTimeout(currentTransactionId, coordinatorState)
}
