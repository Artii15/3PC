package tpc.demo

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import tpc.config.{CoordinatorConfig, WorkerConfig}

import scala.collection.JavaConverters
import scala.concurrent.duration.FiniteDuration

object Main {
  private val applicationConfig = ConfigFactory.load()
  private val timeoutsUnit = applicationConfig.getString("application.timeouts.unit")
  private val workersConfig = readWorkersConfig()
  private val coordinatorConfig = readCoordinatorConfig()

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem(applicationConfig.getString("application.systemName"))
  }

  private def readWorkersConfig(): WorkerConfig = new WorkerConfig {
    override val waitingFinalCommitTimeout: FiniteDuration = readTimeout("application.timeouts.workers.finalCommit")
    override val operationsExecutingTimeout: FiniteDuration = readTimeout("application.timeouts.workers.execution")
    override val waitingForPrepareTimeout: FiniteDuration = readTimeout("application.timeouts.workers.prepare")
  }

  private def readCoordinatorConfig(): CoordinatorConfig = new CoordinatorConfig {
    override val workersConfig: WorkerConfig = Main.workersConfig
    override val cohortLocations: Iterable[String] = readList("application.workers.addresses")
    override val cohortSize: Int = cohortLocations.size
    override val waitingAckTimeout: FiniteDuration = readTimeout("application.timeouts.coordinator.ack")
    override val waitingAgreeTimeout: FiniteDuration = readTimeout("application.timeouts.coordinator.ack")
    override val transactionOperationsTimeout: FiniteDuration = readTimeout("application.timeouts.coordinator.operations")
  }

  private def readTimeout(configKey: String): FiniteDuration =
    FiniteDuration(applicationConfig.getLong(configKey), timeoutsUnit)

  private def readList(configKey: String) =
    JavaConverters.collectionAsScalaIterable(applicationConfig.getStringList(configKey))
}
