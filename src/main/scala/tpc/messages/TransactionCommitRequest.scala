package tpc.messages

import tpc.transactions.ID

case class TransactionCommitRequest(transactionId: ID)
