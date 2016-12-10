package tpc.messages

import java.util.UUID
import tpc.actors.states.CoordinatorState._

case class CoordinatorTimeout(transactionId: Option[UUID], state: CoordinatorState)
