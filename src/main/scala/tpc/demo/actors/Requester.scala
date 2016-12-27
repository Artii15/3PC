package tpc.demo.actors

import akka.actor.{Actor, ActorRef}
import tpc.demo.messages.Start
import tpc.demo.operations.AppendLogOperation
import tpc.messages._
import tpc.messages.transactions.{TransactionBeginRequest, TransactionCommitRequest, TransactionOperations}
import tpc.transactions.ID

import scala.annotation.tailrec
import scala.io.StdIn

class Requester(coordinator: ActorRef) extends Actor {
  private var contentToAppend: String = ""

  override def receive: Receive = {
    case Start => interact()
    case TransactionBeginAck(transactionId) => beginTransaction(transactionId)
    case Abort(_) => println("Transaction aborted"); interact()
    case CommitConfirmation(_) => println("Transaction successfully finished"); interact()
  }

  private def interact(): Unit = {
    println("Type something to commit or q to exit: ")
    StdIn.readLine() match {
      case "q" => context.system.terminate()
      case content: String => sendRequest(content)
    }
  }

  private def sendRequest(content: String): Unit = {
    contentToAppend = content
    coordinator ! TransactionBeginRequest(self)
  }

  private def beginTransaction(transactionId: ID): Unit = {
    coordinator ! TransactionOperations(transactionId, new AppendLogOperation(contentToAppend))
    coordinator ! TransactionCommitRequest(transactionId)
    println("Transaction begun")
  }
}
