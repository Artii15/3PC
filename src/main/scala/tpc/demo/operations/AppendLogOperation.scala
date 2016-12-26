package tpc.demo.operations

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.{Files, Paths, StandardOpenOption}

import tpc.transactions.Operation

class AppendLogOperation(content: String) extends Operation {
  private val temporaryFile = File.createTempFile("tpc-log", ".tmp")
  private val temporaryFilePath = Paths.get(temporaryFile.getAbsolutePath)

  override def execute(): Unit = {
    val fileWriter = new BufferedWriter(new FileWriter(temporaryFile))
    fileWriter.write(content)
  }

  override def rollback(): Unit = cleanUp()

  override def commit(): Unit = {
    Files.write(Paths.get("~/log.txt"), Files.readAllBytes(temporaryFilePath), StandardOpenOption.APPEND)
    cleanUp()
  }

  private def cleanUp(): Unit = {
    temporaryFile.delete()
  }
}
