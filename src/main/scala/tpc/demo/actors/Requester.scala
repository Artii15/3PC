package tpc.demo.actors

import akka.actor.{Actor, ActorRef}
import misra.demo.Writer
import tpc.demo.messages.Start
import tpc.demo.operations.AppendLogOperation
import tpc.messages.transactions._
import tpc.transactions.ID

import scala.io.StdIn

class Requester(coordinator: ActorRef) extends Actor {
  private var contentToAppend: String = ""

  override def receive: Receive = {
    case Start => println("Type something to commit or q to exit: "); interact()
    case TransactionBeginAck(transactionId) => beginTransaction(transactionId)
    case _: Abort | _: CommitConfirmation => interact()
  }

  private def interact(): Unit = {
    StdIn.readLine() match {
      case "q" => context.system.terminate()
      case content: String => Writer.clearScreen(); sendRequest(content)
    }
  }

  private def sendRequest(content: String): Unit = {
    contentToAppend = content
    coordinator ! TransactionBeginRequest(self)
  }

  private def beginTransaction(transactionId: ID): Unit = {
    coordinator ! TransactionOperations(transactionId, new AppendLogOperation(contentToAppend))
    coordinator ! TransactionCommitRequest(transactionId)
  }
}
