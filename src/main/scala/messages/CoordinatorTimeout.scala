package messages

import java.util.UUID
import actors.states.CoordinatorState._

case class CoordinatorTimeout(transactionId: UUID, state: CoordinatorState)