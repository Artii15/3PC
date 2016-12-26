package tpc.messages

import tpc.transactions.ID

case class CommitAgree(transactionId: ID)
