package tpc.config

import scala.concurrent.duration.FiniteDuration

trait CoordinatorConfig {
  val workersConfig: WorkerConfig
  val waitingAgreeTimeout: FiniteDuration
  val waitingAckTimeout: FiniteDuration
  val transactionOperationsTimeout: FiniteDuration
  val cohortSize: Int
  val cohortLocations: Iterable[String]
}
