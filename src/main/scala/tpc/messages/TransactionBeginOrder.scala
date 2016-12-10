package tpc.messages

import java.util.UUID

case class TransactionBeginOrder(transactionId: Option[UUID])
