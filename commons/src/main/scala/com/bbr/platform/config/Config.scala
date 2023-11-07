package com.bbr.platform.config

import enumeratum.{CirisEnum, Enum, EnumEntry}
import eu.timepit.refined.types.string.NonEmptyString

object Config {

  final case class AwsS3Cfg(
    keyId: NonEmptyString,
    password: NonEmptyString,
    region: NonEmptyString,
    bucket: NonEmptyString
  )

  sealed trait AppEnvironment extends EnumEntry

  object AppEnvironment extends Enum[AppEnvironment] with CirisEnum[AppEnvironment] {
    val values: IndexedSeq[AppEnvironment] = findValues

    case object Local extends AppEnvironment

    case object Stage extends AppEnvironment

    case object Production extends AppEnvironment
  }
}
