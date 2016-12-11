package tpc.messages

import tpc.TransactionId

case class TransactionCommitRequest(transactionId: TransactionId)
