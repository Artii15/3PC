package tpc.actors

import akka.actor.Actor
import tpc.{EmptyID, TransactionId, TransactionOperation}
import tpc.actors.states.WorkerState
import tpc.config.WorkerConfig
import tpc.messages._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable

class Worker(config: WorkerConfig) extends Actor {
  import WorkerState._

  var currentTransactionId: TransactionId = EmptyID
  val executedOperations: mutable.MutableList[TransactionOperation] = mutable.MutableList()

  override def receive: Receive = {
    case TransactionBeginOrder(transactionId) => beginTransaction(transactionId)
  }

  private def beginTransaction(transactionId: TransactionId): Unit = {
    currentTransactionId = transactionId
    val timeout = WorkerTimeout(currentTransactionId, WAITING_OPERATIONS)
    context.system.scheduler.scheduleOnce(config.operationsExecutingTimeout, self, timeout)
    context become executingTransaction
  }

  private def executingTransaction: Receive = {
    case TransactionOperations(transactionId, operation) if transactionId == currentTransactionId => executeOperation(operation)
    case TransactionCommitRequest(transactionId) if transactionId == currentTransactionId => waitForPrepare()
    case WorkerTimeout(transactionId, WAITING_OPERATIONS) if transactionId == currentTransactionId => abort()
    case Failure => abort()
    case Abort(transactionId) if transactionId == currentTransactionId => rollback()
  }

  private def executeOperation(operation: TransactionOperation): Unit = {
    try {
      operation.execute()
      executedOperations += operation
    }
    catch {
      case _: Throwable => abort()
    }
  }

  private def waitForPrepare(): Unit = {
    context.parent ! CommitAgree(currentTransactionId)
    val timeout = WorkerTimeout(currentTransactionId, WAITING_PREPARE)
    context.system.scheduler.scheduleOnce(config.waitingForPrepareTimeout, self, timeout)
    context become waitingForPrepare
  }

  private def waitingForPrepare: Receive = {
    case PrepareToCommit(transactionId) if transactionId == currentTransactionId => prepareToCommit()
    case Abort(transactionId) if transactionId == currentTransactionId => rollback()
    case WorkerTimeout(transactionId, WAITING_PREPARE) if transactionId == currentTransactionId => abort()
    case Failure => abort()
  }

  private def prepareToCommit(): Unit = {
    context.parent ! CommitAck(currentTransactionId)

    val timeout = WorkerTimeout(currentTransactionId, WAITING_FINAL_COMMIT)
    context.system.scheduler.scheduleOnce(config.waitingFinalCommitTimeout, self, timeout)

    context become waitingForFinalCommit
  }

  private def waitingForFinalCommit: Receive = {
    case CommitConfirmation(transactionId) if transactionId == currentTransactionId => doCommit()
    case WorkerTimeout(transactionId, WAITING_FINAL_COMMIT) if transactionId == currentTransactionId => doCommit()
    case Failure => doCommit()
    case Abort(transactionId) if transactionId == currentTransactionId => rollback()
  }

  private def doCommit(): Unit = {
    executedOperations.foreach(_.commit())
    cleanUpAfterTransaction()
  }

  private def abort(): Unit = {
    context.parent ! Abort(currentTransactionId)
    rollback()
  }

  private def rollback(): Unit = {
    executedOperations.reverseIterator.foreach(_.rollback())
    cleanUpAfterTransaction()
  }

  private def cleanUpAfterTransaction(): Unit = {
    currentTransactionId = EmptyID
    executedOperations.clear()
    context become receive
  }
}
