package tpc.messages

import tpc.TransactionId

case class CommitAgree(transactionId: TransactionId)
