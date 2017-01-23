package tpc.demo.operations

import java.nio.file.{Files, Paths, StandardOpenOption}
import java.text.SimpleDateFormat
import java.util.Date

import com.typesafe.config.ConfigFactory
import tpc.transactions.Operation

class AppendLogOperation(content: String) extends Operation {
  private var unsavedContent = ""

  override def execute(): Unit = {
    val formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
    unsavedContent = s"$formattedDate\n$content\n\n"
  }

  override def rollback(): Unit = cleanUp()

  override def commit(executorId: Int): Unit = {
      Files.write(Paths.get(s"${AppendLogOperation.logFilePath}$executorId"), unsavedContent.getBytes,
        StandardOpenOption.APPEND, StandardOpenOption.CREATE)
    cleanUp()
  }

  private def cleanUp() = unsavedContent = ""
}

object AppendLogOperation {
  private val logFilePath = ConfigFactory.load().getString("application.logFile")
}
