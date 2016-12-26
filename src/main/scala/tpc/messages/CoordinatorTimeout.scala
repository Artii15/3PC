package tpc.messages

import tpc.actors.states.CoordinatorState._
import tpc.transactions.ID

case class CoordinatorTimeout(transactionId: ID, state: CoordinatorState)
