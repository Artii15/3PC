package tpc.messages.transactions

import akka.actor.ActorRef

case class TransactionBeginRequest(requester: ActorRef)
