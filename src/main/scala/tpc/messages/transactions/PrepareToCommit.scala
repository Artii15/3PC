package tpc.messages.transactions

import tpc.transactions.ID

case class PrepareToCommit(transactionId: ID)
