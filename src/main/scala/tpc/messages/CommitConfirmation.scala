package tpc.messages

import tpc.transactions.ID

case class CommitConfirmation(transactionId: ID)
