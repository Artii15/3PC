package tpc.messages

import tpc.transactions.ID

case class PrepareToCommit(transactionId: ID)
