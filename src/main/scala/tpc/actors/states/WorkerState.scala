package tpc.actors.states

object WorkerState extends Enumeration {
  type WorkerState = Value
  val WAITING_OPERATIONS, WAITING_PREPARE, WAITING_FINAL_COMMIT = Value
}
