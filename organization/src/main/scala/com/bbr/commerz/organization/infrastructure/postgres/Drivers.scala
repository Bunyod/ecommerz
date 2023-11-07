package com.bbr.commerz.organization.infrastructure.postgres

import cats.implicits._
import com.bbr.commerz.organization.domain.branch.BranchPayloads._
import com.bbr.commerz.organization.domain.organization.OrganizationPayloads._
import com.bbr.commerz.organization.domain.owner.OwnerPayloads.Owner
import com.bbr.commerz.organization.domain.staff.StaffPayloads._
import com.bbr.platform.{TimeOpts, UuidOpts}
import com.bbr.platform.domain.Address.Address
import com.bbr.commerz.organization.http.utils.json._
import com.bbr.platform.domain.Branch.BranchId
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff._
import doobie._
import doobie.postgres.implicits._             // DON'T REMOVE IT
import doobie.implicits.javasql._              // DON'T REMOVE IT
import doobie.postgres.circe.jsonb.implicits._ // DON'T REMOVE IT
import io.circe._
import io.circe.syntax._

import java.sql.Timestamp
import java.util.UUID

object Drivers {

  implicit val readBranch: Read[Either[Throwable, Branch]] =
    Read[(UUID, String, UUID, Json, String, UUID, Option[Timestamp])]
      .map { case (id, name, orgId, address, status, createdBy, createdAt) =>
        address
          .as[Address]
          .map { address =>
            Branch(
              BranchId(id),
              orgId.toOrgId,
              BranchName(name),
              address,
              BranchStatus.withName(status),
              createdBy.toStaffId,
              createdAt.map(_.toLocalDateTime.truncateTime)
            )
          }
          .leftMap(error => new Throwable(error.getMessage()))
      }

  implicit val writeBranch: Write[Branch] =
    Write[(UUID, UUID, String, Json, String, UUID, Option[Timestamp])]
      .contramap { branch =>
        (
          branch.id.value,
          branch.orgId.value,
          branch.branchName.value,
          branch.address.asJson,
          branch.status.entryName,
          branch.createdBy.value,
          branch.createdAt.map(Timestamp.valueOf)
        )
      }

  implicit val readOrg: Read[Organization] =
    Read[(UUID, String, String, Option[Timestamp], Option[Timestamp])]
      .map { case (orgId, name, status, createdAt, updatedAt) =>
        Organization(
          id = orgId.toOrgId,
          name = OrganizationName(name),
          status = OrganizationStatus.withName(status),
          createdAt = createdAt.map(_.toLocalDateTime.truncateTime),
          updatedAt = updatedAt.map(_.toLocalDateTime.truncateTime)
        )
      }

  implicit val writeOrg: Write[Organization] =
    Write[(UUID, String, String, Option[Timestamp], Option[Timestamp])]
      .contramap { organization =>
        (
          organization.id.value,
          organization.name.name,
          organization.status.entryName,
          organization.createdAt.map(Timestamp.valueOf),
          organization.updatedAt.map(Timestamp.valueOf)
        )
      }

  implicit val readStaff: Read[Staff] =
    Read[
      (
        UUID,
        UUID,
        Option[UUID],
        String,
        String,
        String,
        Option[String],
        String,
        Option[String],
        Option[String],
        Option[java.sql.Date],
        String,
        Option[Timestamp],
        Option[Timestamp]
      )
    ]
      .map {
        case (
              id,
              orgId,
              branchId,
              role,
              userName,
              password,
              email,
              phoneNumber,
              firstName,
              lastName,
              birthDate,
              status,
              createdAt,
              updatedAt
            ) =>
          Staff(
            id = id.toStaffId,
            orgId = orgId.toOrgId,
            branchId = branchId.map(_.toBranchId),
            role = StaffRole.withName(role),
            userName = UserName(userName),
            password = EncryptedPassword(password),
            email = email.map(Email.apply),
            phoneNumber = PhoneNumber(phoneNumber),
            firstName = firstName.map(FirstName.apply),
            lastName = lastName.map(LastName.apply),
            birthDate = birthDate.map(_.toLocalDate),
            status = StaffStatus.withName(status),
            createdAt = createdAt.map(_.toLocalDateTime.truncateTime),
            updatedAt = updatedAt.map(_.toLocalDateTime.truncateTime)
          )
      }

  implicit val writeStaff: Write[Staff] = Write[
    (
      UUID,
      UUID,
      Option[UUID],
      String,
      String,
      String,
      Option[String],
      String,
      Option[String],
      Option[String],
      Option[java.sql.Date],
      String,
      Option[Timestamp],
      Option[Timestamp]
    )
  ].contramap { staff =>
    (
      staff.id.value,
      staff.orgId.value,
      staff.branchId.map(_.value),
      staff.role.entryName,
      staff.userName.value,
      staff.password.value,
      staff.email.map(_.value),
      staff.phoneNumber.value,
      staff.firstName.map(_.value),
      staff.lastName.map(_.value),
      staff.birthDate.map(t => java.sql.Date.valueOf(t)),
      staff.status.entryName,
      staff.createdAt.map(Timestamp.valueOf),
      staff.updatedAt.map(Timestamp.valueOf)
    )
  }

  implicit val readOwner: Read[Owner] =
    Read[
      (
        UUID,
        Json,
        String,
        String,
        Option[String],
        String,
        Option[String],
        Option[String],
        Option[java.sql.Date],
        String,
        Option[Timestamp]
      )
    ]
      .map {
        case (
              id,
              orgs,
              role,
              password,
              email,
              phoneNumber,
              firstName,
              lastName,
              birthDate,
              status,
              createdAt
            ) =>
          Owner(
            id = id.toStaffId,
            organizations = orgs.as[List[OrgId]].getOrElse(List.empty[OrgId]),
            role = StaffRole.withName(role),
            password = EncryptedPassword(password),
            email = email.map(Email.apply),
            phoneNumber = PhoneNumber(phoneNumber),
            firstName = firstName.map(FirstName.apply),
            lastName = lastName.map(LastName.apply),
            birthDate = birthDate.map(_.toLocalDate),
            status = StaffStatus.withName(status),
            createdAt = createdAt.map(_.toLocalDateTime.truncateTime)
          )
      }

  implicit val writeOwner: Write[Owner] = Write[
    (
      UUID,
      Json,
      String,
      String,
      Option[String],
      String,
      Option[String],
      Option[String],
      Option[java.sql.Date],
      String,
      Option[Timestamp]
    )
  ].contramap { owner =>
    (
      owner.id.value,
      owner.organizations.asJson,
      owner.role.entryName,
      owner.password.value,
      owner.email.map(_.value),
      owner.phoneNumber.value,
      owner.firstName.map(_.value),
      owner.lastName.map(_.value),
      owner.birthDate.map(t => java.sql.Date.valueOf(t)),
      owner.status.entryName,
      owner.createdAt.map(Timestamp.valueOf)
    )
  }

}
