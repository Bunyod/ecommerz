package com.bbr.commerz.auth.http.utils

import com.bbr.commerz.auth.domain.auth.AuthPayloads._
import com.bbr.commerz.organization.domain.organization.OrganizationPayloads.OrganizationNameParam
import com.bbr.commerz.organization.http.utils.refined._
import com.bbr.commerz.organization.http.utils.json._
import com.bbr.platform.domain.Staff.{PasswordParam, PhoneNumber, PhoneNumberParam}
import dev.profunktor.auth.jwt.JwtToken
import eu.timepit.refined.auto._
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe._
import io.circe.refined._
import io.circe.generic.extras.Configuration
import io.circe.generic.semiauto._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

object json extends JsonCodecs {
  implicit def deriveEntityEncoder[F[_], A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
}

trait JsonCodecs {

  // ----- Coercible codecs -----
  implicit def coercibleEncoder[A, B: Encoder]: Encoder[A] =
    Encoder[B].contramap[A](_.asInstanceOf[B])

  implicit def coercibleKeyEncoder[A, B: KeyEncoder]: KeyEncoder[A] =
    KeyEncoder[B].contramap[A](_.asInstanceOf[B])

  implicit val customConfig: Configuration =
    Configuration.default.withDefaults.withSnakeCaseMemberNames.withSnakeCaseConstructorNames

  implicit lazy val passwordParamEncoder: Encoder[PasswordParam] =
    Encoder.forProduct1("password")(_.value)
  implicit lazy val passwordParamDecoder: Decoder[PasswordParam] =
    Decoder.forProduct1("password")(PasswordParam.apply)

  implicit val tokenEncoder: Encoder[JwtToken] = Encoder.forProduct1("access_token")(_.value)

  implicit val loginStaffDecoder: Decoder[LoginStaff] = Decoder.instance { h =>
    for {
      phoneNumber <- h.get[PhoneNumberPred]("phone_number").map(pn => PhoneNumber.apply(pn))
      password    <- h.get[NonEmptyString]("password").map(PasswordParam.apply)
    } yield LoginStaff(phoneNumber, password)
  }

  implicit val loginStaffEncoder: Encoder[LoginStaff] =
    Encoder.forProduct2("phone_number", "password")(ls => (ls.phoneNumber, ls.password))

  implicit val passwordUpdateDecoder: Decoder[PasswordUpdate] = Decoder.instance { h =>
    for {
      oldPassword <- h.get[NonEmptyString]("old_password").map(PasswordParam.apply)
      newPassword <- h.get[NonEmptyString]("new_password").map(PasswordParam.apply)
    } yield PasswordUpdate(oldPassword, newPassword)
  }

  implicit val passwordRecoveryDecoder: Decoder[PasswordRecovery] = Decoder.instance { h =>
    for {
      phoneNumber <- h.get[PhoneNumberPred]("phone_number").map(PhoneNumberParam(_))
      password    <- h.get[NonEmptyString]("password").map(PasswordParam.apply)
      orgName     <- h.get[NonEmptyString]("org_name").map(OrganizationNameParam.apply)
    } yield PasswordRecovery(phoneNumber, password, orgName)
  }

  implicit val recoveryRequestDecoder: Decoder[RecoveryRequest] = Decoder.instance { h =>
    for {
      phoneNumber <- h.get[PhoneNumberPred]("phone_number").map(PhoneNumberParam(_))
      orgName     <- h.get[NonEmptyString]("org_name").map(OrganizationNameParam.apply)
    } yield RecoveryRequest(phoneNumber, orgName)
  }

  implicit val recoveryDataRequestDecoder: Decoder[RecoveryDataRequest] = Decoder.instance { h =>
    for {
      phoneNumber <- h.get[PhoneNumberPred]("phone_number").map(PhoneNumberParam(_))
      code        <- h.get[PosInt]("recovery_code")
    } yield RecoveryDataRequest(phoneNumber, code)
  }

  implicit val claimContentDecoder: Decoder[ClaimContent] = deriveDecoder[ClaimContent]

}
