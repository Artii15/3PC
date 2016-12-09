package actors

import akka.actor.{Actor, ActorRef}
import messages.CommitRequest

class Coordinator(cohort: Traversable[ActorRef]) extends Actor {
  override def receive: Receive = {
    case commitRequest: CommitRequest => receiveCommitRequest(commitRequest)
  }

  private def initializer: Receive = {
    case _ =>
  }

  private def receiveCommitRequest(commitRequest: CommitRequest): Unit = {
    cohort.foreach(_ ! commitRequest)
    context become initializer
  }
}
