package com.bbr.commerz.inventory.domain.unit

import cats.Show
import com.bbr.platform.domain.Organization.OrgId
import enumeratum._
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.{Decoder, Encoder}

import java.util.UUID

object UnitPayloads {

  case class UnitId(value: UUID)

  object UnitId {
    implicit val unitIdEncoder: Encoder[UnitId] = Encoder.forProduct1("unit_id")(_.value)
    implicit val unitIdDecoder: Decoder[UnitId] = Decoder.forProduct1("unit_id")(UnitId.apply)
  }

  case class UnitNameParam(value: NonEmptyString) {
    def toDomain: UnitName = UnitName(value.value)
  }
  case class UnitName(value: String)

  case class UnitRequest(name: UnitNameParam) {
    def toDomain(orgId: OrgId, unitId: UnitId): ProductUnit =
      ProductUnit(
        id = unitId,
        orgId = orgId,
        name = name.toDomain,
        status = UnitStatus.ACTIVE
      )
  }

  case class ProductUnit(
    id: UnitId,
    orgId: OrgId,
    name: UnitName,
    status: UnitStatus
  )

  sealed trait UnitStatus extends EnumEntry

  object UnitStatus extends Enum[UnitStatus] with CirceEnum[UnitStatus] {
    val values: IndexedSeq[UnitStatus] = findValues
    case object ACTIVE   extends UnitStatus
    case object INACTIVE extends UnitStatus
  }

  implicit val showCategory: Show[ProductUnit] = Show.fromToString

}
