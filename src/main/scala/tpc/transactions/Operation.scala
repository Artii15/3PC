package tpc.transactions

trait Operation {
  def execute(): Unit
  def rollback(): Unit
  def commit(executorId: Int): Unit
}
