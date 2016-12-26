package tpc.messages

import tpc.transactions.{ID, Operation}

case class TransactionOperations(transactionId: ID, operation: Operation)
