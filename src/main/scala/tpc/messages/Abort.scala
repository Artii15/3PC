package tpc.messages

import tpc.transactions.ID

case class Abort(transactionId: ID)
