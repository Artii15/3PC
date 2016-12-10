package tpc.storage

trait Storage {
  def accept(storageOperations: StorageOperations): Unit
  def copyFrom(storage: Storage): Unit
}
