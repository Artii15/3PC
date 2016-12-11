package tpc.messages

import tpc.{TransactionId, TransactionOperation}

case class TransactionOperations(transactionId: TransactionId, operation: TransactionOperation)
