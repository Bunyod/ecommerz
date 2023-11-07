package com.bbr.commerz.organization.suite

import cats.Show
import com.bbr.commerz.organization.domain.branch.BranchPayloads._
import com.bbr.commerz.organization.domain.organization.OrganizationPayloads._
import com.bbr.commerz.organization.domain.staff.StaffPayloads._
import com.bbr.platform.domain.Address.{Address, Region, OrgAddress, District}
import com.bbr.platform.domain.Branch.BranchId
import com.bbr.platform.domain.Staff.{StaffRole, StaffId}
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import io.circe.refined._
import io.circe.syntax.EncoderOps
import org.http4s.EntityEncoder
import org.http4s.circe._

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

  implicit val organizationReqEncoder: Encoder[OrganizationRequest] =
    Encoder.forProduct1("name")(_.name.name)

  implicit val branchNameEncoder: Encoder[BranchName] =
    Encoder.forProduct1("name")(_.value)

  implicit val regionEncoder: Encoder[Region] = deriveConfiguredEncoder[Region]

  implicit val districtEncoder: Encoder[District] = deriveConfiguredEncoder[District]

  implicit val orgAddressEncoder: Encoder[OrgAddress] = Encoder.forProduct1("org_address")(_.value)
  
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

  implicit val branchRequestEncoder: Encoder[BranchRequest] = Encoder.forProduct2(
    "name",
    "address"
  )(b => (b.name.value.value, b.address.asJson))

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
  )(b => (b.name.map(_.value.value), b.address.asJson))

  implicit val orgNameEncoder: Encoder[OrganizationName] =
    Encoder.forProduct1("name")(_.name)

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

  implicit val staffRoleEncoder: Encoder[StaffRole] = deriveConfiguredEncoder[StaffRole]

  implicit val staffRequestEncoder: Encoder[StaffRequest] = Encoder.forProduct8(
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
  implicit lazy val firstNameEncoder: Encoder[FirstName] = Encoder.forProduct1("first_name")(_.value)

  implicit lazy val lastNameEncoder: Encoder[LastName] = Encoder.forProduct1("last_name")(_.value)

  implicit val staffIdEncoder: Encoder[StaffId] =
    Encoder.forProduct1("staff_id")(_.value)

  implicit lazy val userNameParamEncoder: Encoder[UserNameParam] =
    Encoder.forProduct1("username")(_.value)

  implicit lazy val emailEncoder: Encoder[Email] = Encoder.forProduct1("email")(_.value)

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
  
  implicit val showOrganization: Show[Organization]           = Show.fromToString
  implicit val showOrganizationReq: Show[OrganizationRequest] = Show.fromToString

  implicit val showBranchId: Show[BranchId]               = Show.fromToString
  implicit val showBranch: Show[Branch]               = Show.fromToString
  implicit val showBranchRequest: Show[BranchRequest] = Show.fromToString
  implicit val showBranchUpdate: Show[BranchUpdate]   = Show.fromToString

}
