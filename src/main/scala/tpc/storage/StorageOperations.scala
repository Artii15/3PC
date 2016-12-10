package tpc.storage

trait StorageOperations {
  def performOn(storage: Storage): Unit
}
