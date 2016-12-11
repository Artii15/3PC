package tpc.messages

import tpc.TransactionId
import tpc.actors.states.WorkerState.WorkerState

case class WorkerTimeout(transactionId: TransactionId, workerState: WorkerState)
