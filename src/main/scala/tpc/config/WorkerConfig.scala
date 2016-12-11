package tpc.config

trait WorkerConfig {
  def getOperationsExecutingTimeout: Int
  def getWaitingForPrepareTimeout: Int
  def getWaitingFinalCommitTimeout: Int
}
