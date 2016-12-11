package tpc.messages

import tpc.TransactionId

case class TransactionBeginAck(transactionId: TransactionId)
