package actors

import akka.actor.Actor
import messages.CommitRequest

class Worker extends Actor {
  override def receive: Receive = {
    case commitRequest: CommitRequest =>
  }
}
