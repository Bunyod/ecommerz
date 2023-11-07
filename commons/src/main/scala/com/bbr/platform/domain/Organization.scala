package com.bbr.platform.domain

import cats.Show
import io.circe.{Decoder, Encoder}

import java.util.UUID

object Organization {

  case class OrgId(value: UUID)
  object OrgId {
    implicit lazy val orgIdDecoder: Decoder[OrgId] = Decoder.forProduct1("org_id")(OrgId.apply)
    implicit lazy val orgIdEncoder: Encoder[OrgId] = Encoder.forProduct1("org_id")(_.value)
    implicit val showOrgId: Show[OrgId]            = Show.fromToString
  }

}
