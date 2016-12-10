package tpc.actors.states

object CoordinatorState extends Enumeration {
  type CoordinatorState = Value
  val INITIALIZING, WAITING_AGREE = Value
}
