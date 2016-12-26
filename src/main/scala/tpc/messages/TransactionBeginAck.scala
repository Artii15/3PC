package tpc.messages

import tpc.transactions.ID

case class TransactionBeginAck(transactionId: ID)
