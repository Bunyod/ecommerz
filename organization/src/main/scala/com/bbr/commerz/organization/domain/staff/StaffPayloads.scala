package com.bbr.commerz.organization.domain.staff

import cats.Show
import com.bbr.commerz.organization.http.utils.refined.{EmailPred, Name}
import com.bbr.platform.domain.Branch.BranchId
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff._
import dev.profunktor.auth.jwt._
import enumeratum._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url
import eu.timepit.refined.types.string.NonEmptyString

import java.time.{LocalDate, LocalDateTime}
import scala.util.control.NoStackTrace

object StaffPayloads {

  case class UserJwtAuth(value: JwtSymmetricAuth)

  case class Email(value: String)
  case class FirstName(value: String)
  case class LastName(value: String)

  case class EmailParam(value: EmailPred) {
    def toDomain: Email = Email(value.value)
  }
  case class FirstNameParam(value: Name)  {
    def toDomain: FirstName = FirstName(value.value)
  }
  case class LastNameParam(value: Name)   {
    def toDomain: LastName = LastName(value.value)
  }

  case class PhoneNumberInUse(phoneNumber: String) extends NoStackTrace

  case class UserName(value: String)
  case class UserNameParam(value: NonEmptyString) {
    def toDomain: UserName = UserName(value.value)
  }

  type URL = String Refined Url

  case class StaffRequest(
    branchId: Option[BranchId],
    role: StaffRole,
    userName: UserNameParam,
    password: PasswordParam,
    email: Option[EmailParam] = None,
    phoneNumber: PhoneNumberParam,
    firstName: Option[FirstNameParam],
    lastName: Option[LastNameParam],
    birthDate: Option[LocalDate]
  ) {
    def toDomain(
      id: StaffId,
      orgId: OrgId,
      password: EncryptedPassword,
      createdAt: Option[LocalDateTime] = None,
      updatedAt: Option[LocalDateTime] = None
    ): Staff =
      Staff(
        id = id,
        orgId = orgId,
        branchId = branchId,
        role = role,
        userName = userName.toDomain,
        password = password,
        email = email.map(_.toDomain),
        phoneNumber = phoneNumber.toDomain,
        firstName = firstName.map(_.toDomain),
        lastName = lastName.map(_.toDomain),
        birthDate = birthDate,
        status = StaffStatus.ACTIVE,
        createdAt = createdAt,
        updatedAt = updatedAt
      )
  }

  case class StaffUpdate(
    branchId: Option[BranchId],
    userName: Option[UserNameParam],
    email: Option[Email],
    phoneNumber: Option[PhoneNumberParam],
    firstName: Option[FirstName],
    lastName: Option[LastName],
    birthDate: Option[LocalDate]
  )

  case class Staff(
    id: StaffId,
    orgId: OrgId,
    branchId: Option[BranchId],
    role: StaffRole,
    userName: UserName,
    password: EncryptedPassword,
    email: Option[Email],
    phoneNumber: PhoneNumber,
    firstName: Option[FirstName],
    lastName: Option[LastName],
    birthDate: Option[LocalDate],
    status: StaffStatus,
    createdAt: Option[LocalDateTime],
    updatedAt: Option[LocalDateTime]
  )

  sealed trait StaffStatus extends EnumEntry
  object StaffStatus       extends Enum[StaffStatus] with CirceEnum[StaffStatus] {
    val values: IndexedSeq[StaffStatus] = findValues
    case object ACTIVE   extends StaffStatus
    case object INACTIVE extends StaffStatus
  }

  implicit val showPerson: Show[Staff]             = Show.fromToString
  implicit val showUserRequest: Show[StaffRequest] = Show.fromToString

}
