package tpc

trait TransactionOperation {
  def execute(): Unit
  def rollback(): Unit
  def commit(): Unit
}
