package actors

import akka.actor.Actor
import messages.TransactionCommitRequest

class Worker extends Actor {
  override def receive: Receive = {
    case commitRequest: TransactionCommitRequest =>
  }
}
