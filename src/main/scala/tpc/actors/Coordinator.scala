package tpc.actors

import java.util.UUID

import akka.actor.{Actor, ActorRef, Address, AddressFromURIString, Deploy, Props}
import akka.remote.RemoteScope
import tpc.{ConcreteID, EmptyID, TransactionId}
import tpc.config.CoordinatorConfig
import tpc.messages._

import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class Coordinator(config: CoordinatorConfig) extends Actor {
  import tpc.actors.states.CoordinatorState._

  private var notAgreedWorkersCount = 0
  private var pendingAck = 0
  private var transactionRequester: Option[ActorRef] = None
  private var currentTransactionId: TransactionId = EmptyID

  override def preStart(): Unit = {
    val cohortLocations = Stream.continually(config.getCohortLocations).flatten.map(AddressFromURIString.apply)
    cohortLocations.take(config.getCohortSize).foreach(deploy)
  }

  private def deploy(address: Address): Unit = context.system
      .actorOf(Props(classOf[Worker], config.getWorkersConfig).withDeploy(Deploy(scope = RemoteScope(address))))

  override def receive: Receive = {
    case TransactionBeginRequest(requester) => beginTransaction(requester)
  }

  private def beginTransaction(requester: ActorRef): Unit = {
    currentTransactionId = ConcreteID(UUID.randomUUID())
    transactionRequester = Some(requester)

    context.children.foreach(_ ! TransactionBeginOrder(currentTransactionId))
    requester ! TransactionBeginAck(currentTransactionId)

    val timeout = CoordinatorTimeout(currentTransactionId, INITIALIZING)
    context.system.scheduler.scheduleOnce(config.getTransactionOperationsTimeout seconds, self, timeout)
    context become initializer
  }

  private def initializer: Receive = {
    case operations: TransactionOperations => executeOperations(operations)
    case commitRequest @ TransactionCommitRequest(transactionId) if transactionId == currentTransactionId =>
      initializeCommit(commitRequest)
    case Failure => abort()
    case CoordinatorTimeout(transactionId, INITIALIZING) if transactionId == currentTransactionId => abort()
    case Abort(transactionId) if transactionId == currentTransactionId => abort()
  }

  private def executeOperations(operations: TransactionOperations): Unit = context.children.foreach(_ ! operations)

  private def initializeCommit(commitRequest: TransactionCommitRequest): Unit = {
    context.children.foreach(_ ! commitRequest)
    notAgreedWorkersCount = config.getCohortSize

    val timeout = CoordinatorTimeout(currentTransactionId, WAITING_AGREE)
    context.system.scheduler.scheduleOnce(config.getWaitingAgreeTimeout seconds, self, timeout)
    context become tryingToWrite
  }

  private def tryingToWrite: Receive = {
    case CommitAgree(transactionId) if transactionId == currentTransactionId => receiveAgree()
    case Failure => abort()
    case CoordinatorTimeout(transactionId, WAITING_AGREE) if transactionId == currentTransactionId  => abort()
    case Abort(transactionId) if transactionId == currentTransactionId => abort()
  }

  private def receiveAgree(): Unit = {
    notAgreedWorkersCount -= 1
    if(notAgreedWorkersCount == 0) {
      context.children.foreach(_ ! PrepareToCommit(currentTransactionId))
      pendingAck = config.getCohortSize

      val timeout = CoordinatorTimeout(currentTransactionId, WAITING_ACK)
      context.system.scheduler.scheduleOnce(config.getWaitingAckTimeout seconds, self, timeout)

      context become preparingToCommit
    }
  }

  private def preparingToCommit: Receive = {
    case CommitAck(transactionId) if transactionId == currentTransactionId => receiveCommitAck()
    case Failure => doCommit()
    case CoordinatorTimeout(transactionId, WAITING_ACK) if transactionId == currentTransactionId => abort()
  }

  private def receiveCommitAck(): Unit = {
    pendingAck -= 1
    if(pendingAck == 0) doCommit()
  }

  private def doCommit(): Unit = {
    context.children.foreach(_ ! CommitConfirmation(currentTransactionId))
    transactionRequester.foreach(_ ! CommitConfirmation(currentTransactionId))
    cleanUpAfterTransaction()
  }

  private def abort(): Unit = {
    context.children.foreach(_ ! Abort(currentTransactionId))
    transactionRequester.foreach(_! Abort(currentTransactionId))
    cleanUpAfterTransaction()
  }

  private def cleanUpAfterTransaction(): Unit = {
    transactionRequester = None
    currentTransactionId = EmptyID
    context become receive
  }
}
