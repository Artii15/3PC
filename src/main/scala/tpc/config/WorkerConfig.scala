package tpc.config

trait WorkerConfig {
  def getOperationsExecutingTimeout: Int
}
