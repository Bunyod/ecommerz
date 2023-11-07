package com.bbr.commerz.inventory.domain.category

import cats._
import com.bbr.platform.domain.Organization.OrgId
import enumeratum._
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.{Decoder, Encoder}

import java.util.UUID

object CategoryPayloads {

  sealed trait CategoryStatus extends EnumEntry

  case class CategoryId(value: UUID)
  object CategoryId                                   {
    implicit val categoryIdEncoder: Encoder[CategoryId] = Encoder.forProduct1("category_id")(_.value)
    implicit val categoryIdDecoder: Decoder[CategoryId] = Decoder.forProduct1("category_id")(CategoryId.apply)
  }
  case class CategoryNameParam(value: NonEmptyString) {
    def toDomain: CategoryName = CategoryName(value.value)
  }

  case class CategoryName(value: String)

  case class CategoryRequest(
    name: CategoryNameParam
  ) {
    def toDomain(orgId: OrgId, id: CategoryId): Category =
      Category(
        id = id,
        orgId = orgId,
        name = name.toDomain,
        status = CategoryStatus.ACTIVE
      )
  }

  case class Category(
    id: CategoryId,
    orgId: OrgId,
    name: CategoryName,
    status: CategoryStatus
  )

  object CategoryStatus extends Enum[CategoryStatus] with CirceEnum[CategoryStatus] {
    val values: IndexedSeq[CategoryStatus] = findValues
    case object ACTIVE   extends CategoryStatus
    case object INACTIVE extends CategoryStatus
  }

  implicit val showCategory: Show[Category] = Show.fromToString

}
