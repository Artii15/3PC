package tpc.messages.transactions

import tpc.transactions.ID

case class CommitAck(transactionId: ID)
