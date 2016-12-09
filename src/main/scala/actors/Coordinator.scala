package actors

import akka.actor.{Actor, ActorRef}
import messages._

class Coordinator(cohort: Traversable[ActorRef]) extends Actor {
  private var notAgreedWorkersCount = cohort.size
  private var pendingAck = notAgreedWorkersCount

  override def receive: Receive = {
    case TransactionBeginRequest() => beginTransaction()
  }

  private def beginTransaction(): Unit = {
    context become initializer
  }

  private def initializer: Receive = {
    case TransactionOperations(operations) => executeOperations(operations)
    case commitRequest: TransactionCommitRequest => initializeCommit(commitRequest)
  }

  private def executeOperations(operations: Unit => Unit): Unit = {
    cohort.foreach(_ ! operations)
  }

  private def initializeCommit(commitRequest: TransactionCommitRequest): Unit = {
    cohort.foreach(_ ! commitRequest)
    context become tryingToWrite
  }

  private def tryingToWrite: Receive = {
    case CommitAgree() => receiveAgree()
  }

  private def receiveAgree(): Unit = {
    notAgreedWorkersCount -= 1
    if(notAgreedWorkersCount == 0) {
      cohort.foreach(_ ! PrepareToCommit())
      context become preparingToCommit
    }
  }

  private def preparingToCommit: Receive = {
    case CommitAck() => receiveCommitAck()
  }

  private def receiveCommitAck(): Unit = {
    pendingAck -= 1
    if(pendingAck == 0) context become receive
  }
}
