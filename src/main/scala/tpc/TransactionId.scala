package tpc

import java.util.UUID

case object TransactionId {
  def areEqual(idOption1: Option[UUID], idOption2: Option[UUID]): Boolean = (for {
    id1 <- idOption1
    id2 <- idOption2
  } yield id1 == id2).getOrElse(false)
}
