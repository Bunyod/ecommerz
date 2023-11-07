package com.bbr.commerz.auth.domain.auth

import com.bbr.commerz.organization.domain.organization.OrganizationPayloads.OrganizationNameParam
import com.bbr.platform.domain.Staff.{PasswordParam, PhoneNumber, PhoneNumberParam}
import eu.timepit.refined.types.numeric.PosInt
import io.circe.Decoder

import java.util.UUID
import scala.util.control.NoStackTrace

object AuthPayloads {
  case class UserNameInUse(username: String)                                     extends NoStackTrace
  case class InvalidUserOrPassword(phoneNumber: String)                          extends NoStackTrace
  case class InvalidVerificationCode(verificationCode: Int, phoneNumber: String) extends NoStackTrace
  case class InvalidRecoveryCode(recoveryCode: Int, phoneNumber: String)         extends NoStackTrace
  case object UnsupportedOperation                                               extends NoStackTrace

  case class PasswordRecovery(
    phoneNumber: PhoneNumberParam,
    password: PasswordParam,
    orgName: OrganizationNameParam
  )

  case class RecoveryRequest(
    phoneNumber: PhoneNumberParam,
    organizationName: OrganizationNameParam
  )

  case class RecoveryDataRequest(
    phoneNumber: PhoneNumberParam,
    recoveryCode: PosInt
  ) {
    def toDomain: RecoveryData = RecoveryData(phoneNumber.toDomain, recoveryCode.value)
  }

  case class RecoveryData(
    phoneNumber: PhoneNumber,
    recoveryCode: Int
  )

  case object TokenNotFound extends NoStackTrace

  case class LoginStaff(
    phoneNumber: PhoneNumber,
    password: PasswordParam
  )

  case class PasswordUpdate(
    oldPassword: PasswordParam,
    newPassword: PasswordParam
  )

  case class ClaimContent(claim: UUID)
  object ClaimContent {
    implicit val claimContentDecoder: Decoder[ClaimContent] = Decoder.forProduct1("claim")(ClaimContent.apply)
  }

}
