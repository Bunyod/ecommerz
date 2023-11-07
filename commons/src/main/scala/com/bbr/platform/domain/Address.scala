package com.bbr.platform.domain

import enumeratum.{CirceEnum, Enum, EnumEntry}
import eu.timepit.refined.types.string.NonEmptyString

object Address {

  case class OrgAddress(value: NonEmptyString)

  sealed trait Region extends EnumEntry

  object Region extends Enum[Region] with CirceEnum[Region] {
    val values: IndexedSeq[Region] = findValues
    case object XORAZM extends Region
  }

  sealed trait District extends EnumEntry
  object District       extends Enum[District] with CirceEnum[District] {
    val values: IndexedSeq[District] = findValues
    case object BAGAT          extends District
    case object GURLAN         extends District
    case object HAZORASP       extends District
    case object SHOVOT         extends District
    case object QOSHKOPIR      extends District
    case object URGANCH        extends District
    case object URGANCH_SHAHAR extends District
    case object XIVA           extends District
    case object XONQA          extends District
    case object YANGIARIQ      extends District
    case object YANGIBOZOR     extends District
  }

  case class Address(
    region: Region,
    district: District,
    orgAddress: OrgAddress,
    guideLine: Option[String]
  )
}
