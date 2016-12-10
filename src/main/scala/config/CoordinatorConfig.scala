package config

trait CoordinatorConfig {
  def getTransactionOperationsTimeout: Int
  def getCohortSize: Int
  def getCohortLocations: Iterable[String]
}
