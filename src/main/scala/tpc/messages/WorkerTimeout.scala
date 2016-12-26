package tpc.messages

import tpc.actors.states.WorkerState.WorkerState
import tpc.transactions.ID

case class WorkerTimeout(transactionId: ID, workerState: WorkerState)
