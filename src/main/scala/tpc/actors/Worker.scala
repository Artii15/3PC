package tpc.actors

import akka.actor.Actor
import tpc.{EmptyID, TransactionId, TransactionOperation}
import tpc.actors.states.WorkerState
import tpc.config.WorkerConfig
import tpc.messages._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable
import scala.concurrent.duration.DurationInt

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
    context.system.scheduler.scheduleOnce(config.getOperationsExecutingTimeout seconds, self, timeout)
    context become executingTransaction
  }

  private def executingTransaction: Receive = {
    case TransactionOperations(operation) => executeOperation(operation)
    case TransactionCommitRequest => waitForCommitDecision()
    case WorkerTimeout(transactionId, WAITING_OPERATIONS) if transactionId == currentTransactionId => abort()
    case Failure => abort()
  }

  private def executeOperation(operation: TransactionOperation): Unit = {
    operation.execute()
    executedOperations += operation
  }

  private def waitForCommitDecision(): Unit = {
    context.parent ! CommitAgree
    val timeout = WorkerTimeout(currentTransactionId, WAITING_PREPARE)
    context.system.scheduler.scheduleOnce(config.getWaitingForPrepareTimeout seconds, self, timeout)
    context become waitingForPrepare
  }

  private def waitingForPrepare: Receive = {
    case PrepareToCommit => prepareToCommit()
    case Abort => abort()
    case WorkerTimeout(transactionId, WAITING_PREPARE) if transactionId == currentTransactionId => abort()
    case Failure => abort()
  }

  private def prepareToCommit(): Unit = {
    context.parent ! CommitAck

    val timeout = WorkerTimeout(currentTransactionId, WAITING_FINAL_COMMIT)
    context.system.scheduler.scheduleOnce(config.getWaitingFinalCommitTimeout seconds, self, timeout)

    context become waitingForFinalCommit
  }

  private def waitingForFinalCommit: Receive = {
    case CommitConfirmation => doCommit()
    case WorkerTimeout(transactionId, WAITING_FINAL_COMMIT) if transactionId == currentTransactionId => doCommit()
    case Failure => doCommit()
    case Abort => abort()
  }

  private def doCommit(): Unit = {
    executedOperations.foreach(_.commit())
  }

  private def abort(): Unit = {
    executedOperations.reverseIterator.foreach(_.rollback())
  }
}
