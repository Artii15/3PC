package tpc.messages.transactions

import tpc.transactions.{ID, Operation}

case class TransactionOperations(transactionId: ID, operation: Operation)
