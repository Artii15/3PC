package tpc.messages

import tpc.TransactionId
import tpc.actors.states.CoordinatorState._

case class CoordinatorTimeout(transactionId: TransactionId, state: CoordinatorState)
