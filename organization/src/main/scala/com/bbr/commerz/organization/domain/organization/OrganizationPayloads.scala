package com.bbr.commerz.organization.domain.organization

import com.bbr.platform.domain.Organization.OrgId
import enumeratum.{CirceEnum, Enum, EnumEntry}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url
import eu.timepit.refined.types.string.NonEmptyString

import java.time.LocalDateTime

object OrganizationPayloads {

  type URL = String Refined Url

  case class OrganizationNameParam(name: NonEmptyString) {
    def toDomain: OrganizationName = OrganizationName(name.value)
  }

  case class OrganizationName(name: String)

  case class OrganizationRequest(
    name: OrganizationNameParam
  ) {
    def toDomain(
      id: OrgId,
      createdAt: Option[LocalDateTime],
      updatedAt: Option[LocalDateTime] = None
    ): Organization =
      Organization(
        id = id,
        name = name.toDomain,
        status = OrganizationStatus.ACTIVE,
        createdAt = createdAt,
        updatedAt = updatedAt
      )
  }

  case class Organization(
    id: OrgId,
    name: OrganizationName,
    status: OrganizationStatus = OrganizationStatus.ACTIVE,
    createdAt: Option[LocalDateTime] = None,
    updatedAt: Option[LocalDateTime] = None
  )

  sealed trait OrganizationStatus extends EnumEntry

  object OrganizationStatus extends Enum[OrganizationStatus] with CirceEnum[OrganizationStatus] {
    val values: IndexedSeq[OrganizationStatus] = findValues
    case object ACTIVE   extends OrganizationStatus
    case object INACTIVE extends OrganizationStatus
  }
}
