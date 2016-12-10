package actors

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props}
import config.CoordinatorConfig
import messages._

class Coordinator(coordinatorConfig: CoordinatorConfig) extends Actor {
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
    transactionRequester = Some(requester)
    currentTransactionId = Some(UUID.randomUUID())
    context become initializer
  }

  private def initializer: Receive = {
    case TransactionOperations(operations) => executeOperations(operations)
    case TransactionCommitRequest => initializeCommit()
  }

  private def executeOperations(operations: Unit => Unit): Unit = context.children.foreach(_ ! operations)

  private def initializeCommit(): Unit = {
    context.children.foreach(_ ! TransactionCommitRequest)
    notAgreedWorkersCount = coordinatorConfig.getCohortSize
    context become tryingToWrite
  }

  private def tryingToWrite: Receive = {
    case CommitAgree => receiveAgree()
  }

  private def receiveAgree(): Unit = {
    notAgreedWorkersCount -= 1
    if(notAgreedWorkersCount == 0) {
      context.children.foreach(_ ! PrepareToCommit)
      pendingAck = coordinatorConfig.getCohortSize
      context become preparingToCommit
    }
  }

  private def preparingToCommit: Receive = {
    case CommitAck() => receiveCommitAck()
  }

  private def receiveCommitAck(): Unit = {
    pendingAck -= 1
    if(pendingAck == 0) {
      transactionRequester.foreach(_ ! CommitConfirmation())
      transactionRequester = None
      context become receive
    }
  }
}
