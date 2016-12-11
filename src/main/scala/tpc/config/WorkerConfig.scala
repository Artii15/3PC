package tpc.config

import scala.concurrent.duration.FiniteDuration

trait WorkerConfig {
  val operationsExecutingTimeout: FiniteDuration
  val waitingForPrepareTimeout: FiniteDuration
  val waitingFinalCommitTimeout: FiniteDuration
}
