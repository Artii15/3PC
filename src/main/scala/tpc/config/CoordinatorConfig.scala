package tpc.config

trait CoordinatorConfig {
  def getWorkersConfig: WorkerConfig
  def getWaitingAgreeTimeout: Int
  def getWaitingAckTimeout: Int
  def getTransactionOperationsTimeout: Int
  def getCohortSize: Int
  def getCohortLocations: Iterable[String]
}
