package tpc.actors

import akka.actor.{Actor, ActorRef}
import tpc.actors.states.WorkerState
import tpc.config.WorkerConfig
import tpc.messages
import tpc.messages.transactions._
import tpc.transactions.{EmptyID, ID, Operation}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

class Worker(config: WorkerConfig, id: Int, logger: ActorRef) extends Actor with Delayed {
  import WorkerState._

  var currentTransactionId: ID = EmptyID
  val executedOperations: mutable.MutableList[Operation] = mutable.MutableList()

  override def receive: Receive = {
    case TransactionBeginOrder(transactionId) => beginTransaction(transactionId)
  }

  private def beginTransaction(transactionId: ID): Unit = {
    currentTransactionId = transactionId
    val timeout = WorkerTimeout(currentTransactionId, WAITING_OPERATIONS)
    context.system.scheduler.scheduleOnce(config.operationsExecutingTimeout, self, timeout)
    logger ! messages.logger.WorkerState(id, WAITING_OPERATIONS.toString)
    suspend(FiniteDuration(1000, "milliseconds"), executingTransaction)
  }

  private def executingTransaction: Receive = {
    case TransactionOperations(transactionId, operation) if transactionId == currentTransactionId => executeOperation(operation)
    case TransactionCommitRequest(transactionId) if transactionId == currentTransactionId => waitForPrepare()
    case WorkerTimeout(transactionId, WAITING_OPERATIONS) if transactionId == currentTransactionId => abort()
    case Failure => abort()
    case Abort(transactionId) if transactionId == currentTransactionId => rollback()
  }

  private def executeOperation(operation: Operation): Unit = {
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
    logger ! messages.logger.WorkerState(id, WAITING_PREPARE.toString)
    suspend(FiniteDuration(1000, "milliseconds"), waitingForPrepare)
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

    logger ! messages.logger.WorkerState(id, WAITING_FINAL_COMMIT.toString)
    suspend(FiniteDuration(1000, "milliseconds"), waitingForFinalCommit)
  }

  private def waitingForFinalCommit: Receive = {
    case CommitConfirmation(transactionId) if transactionId == currentTransactionId => doCommit()
    case WorkerTimeout(transactionId, WAITING_FINAL_COMMIT) if transactionId == currentTransactionId => doCommit()
    case Failure => doCommit()
    case Abort(transactionId) if transactionId == currentTransactionId => rollback()
  }

  private def doCommit(): Unit = {
    executedOperations.foreach(_.commit(id))
    logger ! messages.logger.WorkerState(id, "COMMITTED")
    cleanUpAfterTransaction()
  }

  private def abort(): Unit = {
    context.parent ! Abort(currentTransactionId)
    rollback()
  }

  private def rollback(): Unit = {
    executedOperations.reverseIterator.foreach(_.rollback())
    logger ! messages.logger.WorkerState(id, "ABORTED")
    cleanUpAfterTransaction()
  }

  private def cleanUpAfterTransaction(): Unit = {
    currentTransactionId = EmptyID
    executedOperations.clear()
    suspend(FiniteDuration(1000, "milliseconds"), receive)
  }
}
