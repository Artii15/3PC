package tpc.messages

import tpc.transactions.ID

case class CommitAck(transactionId: ID)
