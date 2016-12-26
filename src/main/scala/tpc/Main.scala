package tpc

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import tpc.actors.Coordinator
import tpc.config.{CoordinatorConfig, WorkerConfig}

import scala.concurrent.duration.FiniteDuration

object Main {
  private val applicationConfig = ConfigFactory.load()
  private val timeoutsUnit = applicationConfig.getString("application.timeouts.unit")
  private val workerConfig = readWorkerConfig()
  private val coordinatorConfig =

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem(applicationConfig.getString("application.systemName"))
  }

  private def readWorkerConfig(): WorkerConfig = new WorkerConfig {
    override val waitingFinalCommitTimeout: FiniteDuration = readTimeout("application.timeouts.workers.finalCommit")
    override val operationsExecutingTimeout: FiniteDuration = readTimeout("application.timeouts.workers.execution")
    override val waitingForPrepareTimeout: FiniteDuration = readTimeout("application.timeouts.workers.prepare")
  }

  private def readCoordinatorConfig(): CoordinatorConfig = new CoordinatorConfig {
    override val workersConfig: WorkerConfig = _
    override val cohortLocations: Iterable[String] = _
    override val cohortSize: Int = _
    override val waitingAckTimeout: FiniteDuration = _
    override val waitingAgreeTimeout: FiniteDuration = _
    override val transactionOperationsTimeout: FiniteDuration = _
  }

  private def readTimeout(configKey: String): FiniteDuration =
    FiniteDuration(applicationConfig.getLong(configKey), timeoutsUnit)
}
