package tpc.demo.operations

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.text.SimpleDateFormat
import java.util.Date

import com.typesafe.config.ConfigFactory
import tpc.transactions.Operation

class AppendLogOperation(content: String) extends Operation {
  private var temporaryFile: Option[File] = None

  override def execute(): Unit = {
    temporaryFile = Some(generateTempFile())
    val fileWriter = new BufferedWriter(new FileWriter(temporaryFile.get))
    val formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
    fileWriter.write(s"$formattedDate\n")
    fileWriter.write(s"$content\n\n")
    fileWriter.close()
  }

  override def rollback(): Unit = cleanUp()

  override def commit(executorId: Int): Unit = {
    temporaryFile.foreach(tempFile => {
      Files.write(Paths.get(s"${AppendLogOperation.logFilePath}$executorId"),
        Files.readAllBytes(Paths.get(tempFile.getAbsolutePath)), StandardOpenOption.APPEND, StandardOpenOption.CREATE)
    })
    cleanUp()
  }

  private def cleanUp(): Unit = temporaryFile.map(_.delete())

  private def generateTempFile() = File.createTempFile("tpc-log", ".tmp")
}

object AppendLogOperation {
  private val logFilePath = ConfigFactory.load().getString("application.logFile")
}
