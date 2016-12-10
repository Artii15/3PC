package messages

import java.util.UUID
import actors.states.CoordinatorState._

case class CoordinatorTimeout(transactionId: Option[UUID], state: CoordinatorState)
