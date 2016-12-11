package tpc

import java.util.UUID

class TransactionId(private val value: Option[UUID]) {
  override def equals(obj: scala.Any): Boolean = obj match {
    case otherId: TransactionId => (for {
      otherIdRealValue <- otherId.value
      thisIdRealValue <- value
    } yield otherIdRealValue == thisIdRealValue).getOrElse(false)
    case _ => false
  }
}

case object EmptyID extends TransactionId(None)
case class ConcreteID(uUID: UUID) extends TransactionId(Some(uUID))
