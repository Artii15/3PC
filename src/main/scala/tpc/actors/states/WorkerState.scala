package tpc.actors.states

object WorkerState extends Enumeration {
  type WorkerState = Value
  val WAITING_OPERATIONS = Value
}
