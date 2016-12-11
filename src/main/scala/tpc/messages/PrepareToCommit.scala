package tpc.messages

import tpc.TransactionId

case class PrepareToCommit(transactionId: TransactionId)
