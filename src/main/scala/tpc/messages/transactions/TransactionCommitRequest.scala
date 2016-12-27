package tpc.messages.transactions

import tpc.transactions.ID

case class TransactionCommitRequest(transactionId: ID)
