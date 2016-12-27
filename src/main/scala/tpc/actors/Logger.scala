package tpc.actors

import akka.actor.Actor
import misra.demo.Writer
import tpc.messages.logger.{CoordinatorState, WorkerState}

import scala.collection.mutable

class Logger extends Actor {
  private val workersStates: mutable.SortedMap[Int, String] = mutable.SortedMap.empty
  private var coordinatorState: String = "UNKNOWN"

  override def receive: Receive = {
    case CoordinatorState(state) => coordinatorState = state; printStates()
    case WorkerState(workerID, state) => workersStates(workerID) = state; printStates()
  }

  private def printStates(): Unit = {
    printRow(1, f"${"Coordinator"}%20s" + workersStates.keys.toList.map(id => f"${s"Worker $id"}%20s").mkString(""))
    printRow(2, f"$coordinatorState%20s" + workersStates.values.map(state => f"$state%20s").mkString(""))
  }

  private def printRow(rowNumber: Int, content: String): Unit = {
    Writer.setCursorPosition(rowNumber, 1)
    Writer.clearLine()
    println(content)
  }
}
