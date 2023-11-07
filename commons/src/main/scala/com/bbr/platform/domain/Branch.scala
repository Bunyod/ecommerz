package com.bbr.platform.domain

import io.circe.{Decoder, Encoder}

import java.util.UUID

object Branch {
  case class BranchId(value: UUID)
  object BranchId {
    implicit val branchIdEncoder: Encoder[BranchId] = Encoder.forProduct1("branch_id")(_.value)
    implicit val branchIdDecoder: Decoder[BranchId] = Decoder.forProduct1("branch_id")(BranchId.apply)
  }
}
