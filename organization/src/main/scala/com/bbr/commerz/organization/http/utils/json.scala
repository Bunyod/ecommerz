package com.bbr.commerz.organization.http.utils

import cats._
import com.bbr.commerz.organization.domain.branch.BranchPayloads._
import com.bbr.commerz.organization.domain.organization.OrganizationPayloads._
import com.bbr.commerz.organization.domain.staff.StaffPayloads._
import com.bbr.commerz.organization.http.utils.refined._
import com.bbr.platform.domain.Address.{Address, District, OrgAddress, Region}
import com.bbr.platform.domain.Branch.BranchId
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff._
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.auto._
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.refined._
import io.circe.syntax._
import org.http4s.EntityEncoder
import org.http4s.circe._

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

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

  implicit val usernameEncoder: Encoder[UserName] = Encoder.forProduct1("username")(_.value)
  implicit val usernameDecoder: Decoder[UserName] = Decoder.forProduct1("username")(UserName.apply)

  implicit val firstNameParamEncoder: Encoder[FirstNameParam] =
    Encoder.forProduct1("first_name")(_.value)
  implicit val firstNameParamDecoder: Decoder[FirstNameParam] =
    Decoder.forProduct1("first_name")(FirstNameParam.apply)

  implicit val lastNameParamEncoder: Encoder[LastNameParam] =
    Encoder.forProduct1("last_name")(_.value)
  implicit val lastNameParamDecoder: Decoder[LastNameParam] =
    Decoder.forProduct1("last_name")(LastNameParam.apply)

  implicit val staffAuthEncoder: Encoder[StaffAuth] =
    Encoder.forProduct4("staff_id", "branch_id", "phone_number", "role")(sa =>
      (sa.id.value, sa.branchId.map(_.value), sa.phoneNumber.value, sa.role.entryName)
    )
  implicit val staffAuthDecoder: Decoder[StaffAuth] =
    Decoder.instance { h =>
      for {
        id          <- h.get[UUID]("staff_id").map(StaffId.apply)
        branchId    <- h.downField("branch_id").as[Option[UUID]].map(_.map(BranchId.apply))
        phoneNumber <- h.get[PhoneNumberPred]("phone_number").map(pn => PhoneNumber(pn))
        role        <- h.get[String]("role").map(s => StaffRole.withName(s))
      } yield StaffAuth(id, branchId, phoneNumber, role)
    }

  implicit val staffRoleEncoder: Encoder[StaffRole] = deriveConfiguredEncoder[StaffRole]
  implicit val staffRoleDecoder: Decoder[StaffRole] = deriveConfiguredDecoder[StaffRole]

  implicit val staffRequestEncoder: Encoder[StaffRequest] = Encoder.forProduct9(
    "branch_id",
    "role",
    "user_name",
    "password",
    "email",
    "phone_number",
    "first_name",
    "last_name",
    "birth_date"
  )(sr =>
    (
      sr.branchId.map(_.value),
      sr.role.entryName,
      sr.userName.value,
      sr.password.value,
      sr.email.map(_.value),
      sr.phoneNumber.value,
      sr.firstName.map(_.value),
      sr.lastName.map(_.value),
      sr.birthDate
    )
  )
  implicit val staffRequestDecoder: Decoder[StaffRequest] = Decoder.instance { h =>
    for {
      branchId    <- h.downField("branch_id").as[Option[UUID]].map(_.map(BranchId.apply))
      role        <- h.get[String]("role").map(StaffRole.withName)
      username    <- h.get[NonEmptyString]("user_name").map(UserNameParam.apply)
      password    <- h.get[NonEmptyString]("password").map(PasswordParam.apply)
      phoneNumber <- h.get[PhoneNumberPred]("phone_number").map(pn => PhoneNumberParam(pn))
      email       <- h.downField("email").as[Option[EmailPred]].map(_.map(EmailParam.apply))
      firstName   <- h.downField("first_name").as[Option[Name]].map(_.map(FirstNameParam.apply))
      lastName    <- h.downField("last_name").as[Option[Name]].map(_.map(LastNameParam.apply))
      birthDate   <- h.downField("birth_date").as[Option[LocalDate]]
    } yield StaffRequest(branchId, role, username, password, email, phoneNumber, firstName, lastName, birthDate)
  }

  implicit val staffUpdateDecoder: Decoder[StaffUpdate] = Decoder.instance { h =>
    for {
      branchId    <- h.downField("branch_id").as[Option[UUID]].map(_.map(BranchId.apply))
      username    <- h.downField("user_name").as[Option[NonEmptyString]].map(_.map(UserNameParam.apply))
      email       <- h.downField("email").as[Option[EmailPred]].map(_.map(e => Email(e)))
      phoneNumber <- h.downField("phone_number").as[Option[PhoneNumberPred]].map(_.map(s => PhoneNumberParam(s)))
      firstName   <- h.downField("first_name").as[Option[Name]].map(_.map(n => FirstName(n)))
      lastName    <- h.downField("last_name").as[Option[Name]].map(_.map(n => LastName(n)))
      birthDate   <- h.downField("birth_date").as[Option[LocalDate]]
    } yield StaffUpdate(branchId, username, email, phoneNumber, firstName, lastName, birthDate)
  }
  implicit val staffUpdateEncoder: Encoder[StaffUpdate] = Encoder.forProduct7(
    "branch_id",
    "user_name",
    "email",
    "phone_number",
    "first_name",
    "last_name",
    "birth_date"
  )(s =>
    (
      s.branchId.map(_.value),
      s.userName.map(_.value.value),
      s.email.map(_.value),
      s.phoneNumber.map(_.value),
      s.firstName.map(_.value),
      s.lastName.map(_.value),
      s.birthDate
    )
  )

  implicit val staffStatusEncoder: Encoder[StaffStatus] = deriveConfiguredEncoder[StaffStatus]
  implicit val staffStatusDecoder: Decoder[StaffStatus] = deriveConfiguredDecoder[StaffStatus]

  implicit val encryptedPasswordEncoder: Encoder[EncryptedPassword] = deriveEncoder[EncryptedPassword]
  implicit val encryptedPasswordDecoder: Decoder[EncryptedPassword] = deriveDecoder[EncryptedPassword]

  implicit lazy val firstNameEncoder: Encoder[FirstName] = Encoder.forProduct1("first_name")(_.value)
  implicit lazy val firstNameDecoder: Decoder[FirstName] = Decoder.forProduct1("first_name")(FirstName.apply)

  implicit lazy val lastNameEncoder: Encoder[LastName] = Encoder.forProduct1("last_name")(_.value)
  implicit lazy val lastNameDecoder: Decoder[LastName] = Decoder.forProduct1("last_name")(LastName.apply)

  implicit val staffDecoder: Decoder[Staff] = Decoder.instance { h =>
    for {
      id          <- h.get[UUID]("id").map(StaffId.apply)
      orgId       <- h.get[UUID]("org_id").map(OrgId.apply)
      branchId    <- h.downField("branch_id").as[Option[UUID]].map(_.map(BranchId.apply))
      role        <- h.get[String]("role").map(s => StaffRole.withName(s))
      userName    <- h.get[String]("user_name").map(UserName.apply)
      password    <- h.get[String]("password").map(EncryptedPassword.apply)
      email       <- h.downField("email").as[Option[String]].map(_.map(Email.apply))
      phoneNumber <- h.get[String]("phone_number").map(PhoneNumber.apply)
      firstName   <- h.downField("first_name").as[Option[String]].map(_.map(FirstName.apply))
      lastName    <- h.downField("last_name").as[Option[String]].map(_.map(LastName.apply))
      birthDate   <- h.downField("birth_date").as[Option[LocalDate]]
      status      <- h.get[String]("status").map(s => StaffStatus.withName(s))
      createdAt   <- h.downField("created_at").as[Option[LocalDateTime]]
      updatedAt   <- h.downField("updated_at").as[Option[LocalDateTime]]
    } yield Staff(
      id = id,
      orgId = orgId,
      branchId = branchId,
      role = role,
      userName = userName,
      password = password,
      email = email,
      phoneNumber = phoneNumber,
      firstName = firstName,
      lastName = lastName,
      birthDate = birthDate,
      status = status,
      createdAt = createdAt,
      updatedAt = updatedAt
    )
  }
  implicit val staffEncoder: Encoder[Staff] = Encoder.forProduct13(
    "id",
    "org_id",
    "role",
    "user_name",
    "password",
    "email",
    "phone_number",
    "first_name",
    "last_name",
    "birth_date",
    "status",
    "created_at",
    "updated_at"
  )(s =>
    (
      s.id.value,
      s.orgId.value,
      s.role.entryName,
      s.userName.value,
      s.password.value,
      s.email.map(_.value),
      s.phoneNumber.value,
      s.firstName.map(_.value),
      s.lastName.map(_.value),
      s.birthDate,
      s.status.entryName,
      s.createdAt,
      s.updatedAt
    )
  )

  implicit val showStaffId: Show[StaffId] = Show.fromToString

  implicit val staffIdEncoder: Encoder[StaffId] =
    Encoder.forProduct1("staff_id")(_.value)

  implicit val staffIdDecoder: Decoder[StaffId] =
    Decoder.forProduct1("staff_id")(StaffId.apply)

  implicit lazy val userNameParamEncoder: Encoder[UserNameParam] =
    Encoder.forProduct1("username")(_.value)
  implicit lazy val userNameParamDecoder: Decoder[UserNameParam] =
    Decoder.forProduct1("username")(UserNameParam.apply)

  implicit lazy val emailDecoder: Decoder[Email] = Decoder.forProduct1("email")(Email.apply)
  implicit lazy val emailEncoder: Encoder[Email] = Encoder.forProduct1("email")(_.value)

  implicit val organizationStatusEncoder: Encoder[OrganizationStatus] = deriveConfiguredEncoder[OrganizationStatus]
  implicit val organizationStatusDecoder: Decoder[OrganizationStatus] = deriveConfiguredDecoder[OrganizationStatus]

  implicit val branchStatusEncoder: Encoder[BranchStatus] = deriveConfiguredEncoder[BranchStatus]
  implicit val branchStatusDecoder: Decoder[BranchStatus] = deriveConfiguredDecoder[BranchStatus]

  implicit val branchNameEncoder: Encoder[BranchName] =
    Encoder.forProduct1("name")(_.value)
  implicit val branchNameDecoder: Decoder[BranchName] =
    Decoder.forProduct1("name")(BranchName.apply)

  implicit val branchNameParamEncoder: Encoder[BranchNameParam] =
    Encoder.forProduct1("name")(_.value)
  implicit val branchNameParamDecoder: Decoder[BranchNameParam] =
    Decoder.forProduct1("name")(BranchNameParam.apply)

  implicit val branchIdEncoder: Encoder[BranchId] =
    Encoder.forProduct1("branch_id")(_.value)
  implicit val branchIdDecoder: Decoder[BranchId] =
    Decoder.forProduct1("branch_id")(BranchId.apply)

  implicit val orgNameEncoder: Encoder[OrganizationName] =
    Encoder.forProduct1("name")(_.name)
  implicit val orgNameDecoder: Decoder[OrganizationName] =
    Decoder.forProduct1("name")(OrganizationName.apply)

  implicit val organizationReqEncoder: Encoder[OrganizationRequest] =
    Encoder.forProduct1("name")(_.name.name.value)

  implicit val organizationReqDecoder: Decoder[OrganizationRequest] = Decoder.instance { h =>
    for {
      name <- h.get[NonEmptyString]("name").map(OrganizationNameParam.apply)
    } yield OrganizationRequest(name)
  }

  implicit val organizationEncoder: Encoder[Organization] = Encoder.forProduct5(
    "id",
    "name",
    "status",
    "created_at",
    "updated_at"
  )(o =>
    (
      o.id.value,
      o.name.name,
      o.status.entryName,
      o.createdAt,
      o.updatedAt
    )
  )

  implicit val regionEncoder: Encoder[Region] = deriveConfiguredEncoder[Region]
  implicit val regionDecoder: Decoder[Region] = deriveConfiguredDecoder[Region]

  implicit val districtEncoder: Encoder[District] = deriveConfiguredEncoder[District]
  implicit val districtDecoder: Decoder[District] = deriveConfiguredDecoder[District]

  implicit val orgAddressEncoder: Encoder[OrgAddress] = Encoder.forProduct1("org_address")(_.value)
  implicit val orgAddressDecoder: Decoder[OrgAddress] = Decoder.forProduct1("org_address")(OrgAddress.apply)

  implicit val addressDecoder: Decoder[Address] = Decoder.instance { h =>
    for {
      r  <- h.get[String]("region").map(v => Region.withName(v))
      c  <- h.get[String]("district").map(v => District.withName(v))
      oa <- h.get[NonEmptyString]("org_address").map(OrgAddress.apply)
      gl <- h.downField("guide_line").as[Option[String]]
    } yield Address(r, c, oa, gl)
  }

  implicit val addressEncoder: Encoder[Address] = Encoder.forProduct4(
    "region",
    "district",
    "org_address",
    "guide_line"
  )(a =>
    (
      a.region.entryName,
      a.district.entryName,
      a.orgAddress.value.value,
      a.guideLine
    )
  )

  implicit val organizationDecoder: Decoder[Organization] = Decoder.instance { h =>
    for {
      id        <- h.get[UUID]("id").map(OrgId.apply)
      name      <- h.get[NonEmptyString]("name").map(name => OrganizationName(name))
      status    <- h.get[String]("status").map(s => OrganizationStatus.withName(s))
      createdAt <- h.downField("created_at").as[Option[LocalDateTime]]
      updatedAt <- h.downField("updated_at").as[Option[LocalDateTime]]
    } yield Organization(id = id, name = name, status = status, createdAt = createdAt, updatedAt = updatedAt)
  }

  implicit val branchRequestEncoder: Encoder[BranchRequest] = Encoder.forProduct2(
    "name",
    "address"
  )(b => (b.name.value, b.address.asJson))

  implicit val branchRequestDecoder: Decoder[BranchRequest] = Decoder.instance { h =>
    for {
      name    <- h.get[NonEmptyString]("name").map(name => BranchNameParam(name))
      address <- h.get[Address]("address")
    } yield BranchRequest(name, address)
  }

  implicit val branchDecoder: Decoder[Branch] = Decoder.instance { h =>
    for {
      id        <- h.get[UUID]("branch_id").map(BranchId.apply)
      orgId     <- h.get[UUID]("org_id").map(OrgId.apply)
      name      <- h.get[NonEmptyString]("branch_name").map(name => BranchName(name))
      address   <- h.get[Address]("address")
      status    <- h.get[String]("status").map(s => BranchStatus.withName(s))
      createdBy <- h.get[UUID]("created_by").map(StaffId.apply)
      createdAt <- h.downField("created_at").as[Option[LocalDateTime]]
    } yield Branch(
      id = id,
      orgId = orgId,
      branchName = name,
      address = address,
      status = status,
      createdBy = createdBy,
      createdAt = createdAt
    )
  }

  implicit val branchEncoder: Encoder[Branch] = Encoder.forProduct7(
    "branch_id",
    "org_id",
    "branch_name",
    "address",
    "status",
    "created_by",
    "created_at"
  )(b =>
    (
      b.id.value,
      b.orgId.value,
      b.branchName.value,
      b.address.asJson,
      b.status.entryName,
      b.createdBy.value,
      b.createdAt
    )
  )

  implicit val branchUpdateEncoder: Encoder[BranchUpdate] = Encoder.forProduct2(
    "branch_name",
    "address"
  )(b =>
    (
      b.name.map(_.value),
      b.address.asJson
    )
  )

  implicit val branchUpdateDecoder: Decoder[BranchUpdate] = Decoder.instance { h =>
    for {
      name    <- h.downField("branch_name").as[Option[NonEmptyString]].map(_.map(BranchNameParam.apply))
      address <- h.downField("address").as[Option[Address]]
    } yield BranchUpdate(
      name = name,
      address = address
    )
  }

  implicit val showOrganization: Show[Organization]               = Show.fromToString
  implicit val showOrganizationRequest: Show[OrganizationRequest] = Show.fromToString
}
