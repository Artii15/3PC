package tpc.messages

import tpc.TransactionId

case class CommitConfirmation(transactionId: TransactionId)
