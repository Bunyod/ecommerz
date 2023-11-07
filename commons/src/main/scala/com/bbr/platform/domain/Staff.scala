package com.bbr.platform.domain

import com.bbr.platform.domain.Branch.BranchId
import enumeratum._
import eu.timepit.refined.types.string.NonEmptyString
import io.circe._
import io.circe.refined._

import java.util.UUID

object Staff {

  case class StaffId(value: UUID)

  case class Password(value: String)
  case class PasswordParam(value: NonEmptyString) {
    def toDomain: Password = Password(value.value)
  }

  case class EncryptedPassword(value: String)

  sealed trait StaffRole extends EnumEntry
  object StaffRole       extends Enum[StaffRole] with CirceEnum[StaffRole] {
    val values: IndexedSeq[StaffRole] = findValues
    case object OWNER  extends StaffRole
    case object WORKER extends StaffRole
    case object AGENT  extends StaffRole
    case object ADMIN  extends StaffRole
  }

  case class PhoneNumber(value: String)
  object PhoneNumber {
    implicit val phoneNumberEncoder: Encoder[PhoneNumber] =
      Encoder.forProduct1("phone_number")(_.value)
    implicit val phoneNumberDecoder: Decoder[PhoneNumber] =
      Decoder.forProduct1("phone_number")(PhoneNumber.apply)
  }

  case class PhoneNumberParam(value: NonEmptyString) {
    def toDomain: PhoneNumber = PhoneNumber(value.value)
  }
  object PhoneNumberParam                            {
    implicit val phoneNumberParamEncoder: Encoder[PhoneNumberParam] =
      Encoder.forProduct1("phone_number")(_.value.value)
    implicit val phoneNumberParamDecoder: Decoder[PhoneNumberParam] =
      Decoder.forProduct1("phone_number")(PhoneNumberParam.apply)
  }

  case class StaffAuth(
    id: StaffId,
    branchId: Option[BranchId],
    phoneNumber: PhoneNumber,
    role: StaffRole
  )
}
