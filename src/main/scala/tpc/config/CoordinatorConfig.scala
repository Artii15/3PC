package tpc.config

trait CoordinatorConfig {
  def getWaitingAgreeTimeout: Int
  def getTransactionOperationsTimeout: Int
  def getCohortSize: Int
  def getCohortLocations: Iterable[String]
}
