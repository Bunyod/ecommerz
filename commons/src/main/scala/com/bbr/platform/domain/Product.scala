package com.bbr.platform.domain

import cats.Show
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.util.UUID

object Product {
  case class ProductId(value: UUID)
  object ProductId {
    implicit val productIdDecoder: Decoder[ProductId] = deriveDecoder[ProductId]
    implicit val productIdEncoder: Encoder[ProductId] = deriveEncoder[ProductId]
    implicit val productIdShow: Show[ProductId]       = Show.fromToString
  }

}
