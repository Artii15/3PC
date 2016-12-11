package tpc.messages

import java.util.UUID

import tpc.actors.states.WorkerState.WorkerState

case class WorkerTimeout(transactionId: Option[UUID], workerState: WorkerState)
