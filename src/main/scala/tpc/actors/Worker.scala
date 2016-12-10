package tpc.actors

import akka.actor.Actor
import tpc.messages.TransactionCommitRequest

class Worker extends Actor {
  override def receive: Receive = {
    case TransactionCommitRequest =>
  }
}
