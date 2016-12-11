package tpc.messages

import tpc.TransactionId

case class CommitAck(transactionId: TransactionId)
