package tpc.transactions

import java.util.UUID

class ID(private val value: Option[UUID]) {
  override def equals(obj: scala.Any): Boolean = obj match {
    case otherId: ID => (for {
      otherIdRealValue <- otherId.value
      thisIdRealValue <- value
    } yield otherIdRealValue == thisIdRealValue).getOrElse(false)
    case _ => false
  }
}

case object EmptyID extends ID(None)
case class ConcreteID(uUID: UUID) extends ID(Some(uUID))
