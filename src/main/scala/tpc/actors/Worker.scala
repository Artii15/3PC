package tpc.actors

import java.util.UUID

import akka.actor.Actor
import tpc.TransactionOperation
import tpc.messages._

class Worker extends Actor {
  var currentTransactionId: Option[UUID] = None
  var executedOperations: List[TransactionOperation] = Nil

  override def receive: Receive = {
    case TransactionBeginOrder(transactionId) => beginTransaction(transactionId)
  }

  private def beginTransaction(transactionId: Option[UUID]): Unit = {
    currentTransactionId = transactionId
    context become executingTransaction
  }

  private def executingTransaction: Receive = {
    case TransactionOperations(operation) => executeOperation(operation)
    case TransactionCommitRequest => waitForCommitDecision()
  }

  private def executeOperation(operation: TransactionOperation): Unit = {
    operation.execute()
    executedOperations = operation :: executedOperations
  }

  private def waitForCommitDecision(): Unit = {
    context.parent ! CommitAgree
    context become waitingForCommitDecision
  }

  private def waitingForCommitDecision: Receive = {
    case PrepareToCommit => prepareToCommit()
  }

  private def prepareToCommit(): Unit = {
    context.parent ! CommitAck
    context become waitingForFinalCommit
  }

  private def waitingForFinalCommit: Receive = {
    case CommitConfirmation => doCommit()
  }

  private def doCommit(): Unit = {

  }
}
