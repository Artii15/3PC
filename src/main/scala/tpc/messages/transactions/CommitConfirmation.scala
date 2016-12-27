package tpc.messages.transactions

import tpc.transactions.ID

case class CommitConfirmation(transactionId: ID)
