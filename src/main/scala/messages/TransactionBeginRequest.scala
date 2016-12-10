package messages

import akka.actor.ActorRef

case class TransactionBeginRequest(requester: ActorRef)
