package tpc.messages.transactions

import tpc.transactions.ID

case class Abort(transactionId: ID)
