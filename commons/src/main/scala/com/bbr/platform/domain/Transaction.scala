package com.bbr.platform.domain

import io.circe._

import java.util.UUID

object Transaction {

  case class TransactionId(value: UUID)

  object TransactionId {

    implicit val transactionIdEncoder: Encoder[TransactionId] =
      Encoder.forProduct1("transaction_id")(_.value)

    implicit val transactionIdDecoder: Decoder[TransactionId] =
      Decoder.forProduct1("transaction_id")(TransactionId.apply)
  }
}
