package com.bbr.commerz.organization.infrastructure.postgres

import cats.effect._
import cats.implicits._
import com.bbr.commerz.organization.domain.staff.StaffPayloads._
import com.bbr.commerz.organization.domain.staff.StaffAlgebra
import com.bbr.platform.getCurrentTime
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff._
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._ // DON'T REMOVE IT
import doobie.implicits.javasql._  // DON'T REMOVE IT
import doobie.util.transactor.Transactor
import doobie.util.update.Update0

import java.sql.Timestamp
import java.util.UUID

class StaffRepository[F[_]: Async](tr: Transactor[F]) extends StaffAlgebra[F] {

  import StaffRepository._

  override def checkPhoneNumber(orgId: OrgId, phoneNumber: PhoneNumber): F[Boolean] =
    select(orgId, phoneNumber).unique.transact(tr)

  override def create(staff: Staff): F[Staff] =
    insert(staff).run.transact(tr) *> getById(staff.orgId, staff.id)

  override def getWorkers(orgId: OrgId, username: Option[String], limit: Int, offset: Int): F[List[Staff]] =
    username match {
      case Some(name) => selectWorkers(orgId.value, name, limit, offset).to[List].transact(tr)
      case None       => selectWorkers(orgId.value, limit, offset).to[List].transact(tr)
    }

  override def getAgents(orgId: OrgId, username: Option[String], limit: Int, offset: Int): F[List[Staff]] =
    username match {
      case Some(name) => selectAgents(orgId.value, name, limit, offset).to[List].transact(tr)
      case None       => selectAgents(orgId.value, limit, offset).to[List].transact(tr)
    }

  override def getAll(orgId: OrgId, username: Option[String], limit: Int, offset: Int): F[List[Staff]] =
    username match {
      case Some(name) => selectAll(orgId.value, name, limit, offset).to[List].transact(tr)
      case None       => selectAll(orgId.value, limit, offset).to[List].transact(tr)
    }

  override def getById(orgId: OrgId, staffId: StaffId): F[Staff] =
    selectById(orgId.value, staffId.value).option.transact(tr).flatMap {
      case Some(staff) => staff.pure[F]
      case None        => new Throwable(s"Could not find staff with this ID: $staffId").raiseError[F, Staff]
    }

  override def updateById(staffId: StaffId, staff: Staff): F[Staff] =
    update(staffId.value, staff).run.transact(tr) *> getById(staff.orgId, staffId)

  override def deleteById(orgId: OrgId, staffId: StaffId): F[String] =
    delete(orgId.value, staffId.value).run.transact(tr).flatMap {
      case 0 => new Throwable(s"Could not find staff with this ID: $staffId").raiseError[F, String]
      case _ => "Successfully deleted".pure[F]
    }
}

object StaffRepository {

  import Drivers._

  private def select(orgId: OrgId, phoneNumber: PhoneNumber): Query0[Boolean] =
    sql"""SELECT EXISTS(SELECT PHONE_NUMBER FROM STAFF
         WHERE ORG_ID = ${orgId.value} AND
         PHONE_NUMBER = ${phoneNumber.value})"""
      .query[Boolean]

  private def selectById(orgId: UUID, staffId: UUID): Query0[Staff] =
    sql"""SELECT * FROM STAFF WHERE
         ID = $staffId AND
         ORG_ID = $orgId AND
         STATUS = ${StaffStatus.ACTIVE.entryName}""".query[Staff]

  private def selectWorkers(
    orgId: UUID,
    username: String,
    limit: Int,
    offset: Int
  ): Query0[Staff] =
    sql"""
          SELECT * FROM STAFF WHERE
          ORG_ID = $orgId AND
          USERNAME = $username AND
          ROLE = ${StaffRole.WORKER.entryName} AND
          STATUS = ${StaffStatus.ACTIVE.entryName}
          LIMIT $limit
          OFFSET $offset
          """.query

  private def selectAgents(orgId: UUID, username: String, limit: Int, offset: Int): Query0[Staff] =
    sql"""
          SELECT * FROM STAFF WHERE
          ORG_ID = $orgId AND
          USERNAME = $username AND
          ROLE = ${StaffRole.AGENT.entryName} AND
          STATUS = ${StaffStatus.ACTIVE.entryName}
          LIMIT $limit
          OFFSET $offset
          """.query

  private def selectAll(orgId: UUID, limit: Int, offset: Int): Query0[Staff] =
    sql"""
          SELECT * FROM STAFF WHERE
          ORG_ID = $orgId AND
          STATUS = ${StaffStatus.ACTIVE.entryName}
          LIMIT $limit
          OFFSET $offset
          """.query

  private def selectAll(orgId: UUID, username: String, limit: Int, offset: Int): Query0[Staff] =
    sql"""
          SELECT * FROM STAFF WHERE
          ORG_ID = $orgId AND
          USERNAME = $username AND
          STATUS = ${StaffStatus.ACTIVE.entryName}
          LIMIT $limit
          OFFSET $offset
          """.query

  private def insert(staff: Staff): Update0 =
    sql"""
      INSERT INTO STAFF (ID, ORG_ID, BRANCH_ID, ROLE, USERNAME, PASSWORD, EMAIL, PHONE_NUMBER, FIRSTNAME, LASTNAME, BIRTHDATE,
      STATUS, CREATED_AT, UPDATED_AT)
      VALUES (
        ${staff.id.value},
        ${staff.orgId.value},
        ${staff.branchId.map(_.value)},
        ${staff.role.entryName},
        ${staff.userName.value},
        ${staff.password.value},
        ${staff.email.map(_.value)},
        ${staff.phoneNumber.value},
        ${staff.firstName.map(_.value)},
        ${staff.lastName.map(_.value)},
        ${staff.birthDate.map(bd => java.sql.Date.valueOf(bd))},
        ${staff.status.entryName},
        ${staff.createdAt.map(Timestamp.valueOf)},
        ${staff.updatedAt.map(Timestamp.valueOf)}
         )
      """.update

  private def selectWorkers(orgId: UUID, limit: Int, offset: Int): Query0[Staff] =
    sql"""
          SELECT * FROM STAFF WHERE
          ORG_ID = $orgId AND
          ROLE = ${StaffRole.WORKER.entryName} AND
          STATUS = ${StaffStatus.ACTIVE.entryName}
          LIMIT $limit
          OFFSET $offset
          """.query[Staff]

  private def selectAgents(orgId: UUID, limit: Int, offset: Int): Query0[Staff] =
    sql"""
          SELECT * FROM STAFF WHERE
          ORG_ID = $orgId AND
          ROLE = ${StaffRole.AGENT.entryName} AND
          STATUS = ${StaffStatus.ACTIVE.entryName}
          LIMIT $limit
          OFFSET $offset
          """.query[Staff]

  private def update(
    staffId: UUID,
    staff: Staff
  ): Update0 =
    sql"""
         UPDATE STAFF SET
         USERNAME = ${staff.userName.value},
         BRANCH_ID = ${staff.branchId.map(_.value)},
         EMAIL = ${staff.email.map(_.value)},
         PHONE_NUMBER = ${staff.phoneNumber.value},
         FIRSTNAME = ${staff.firstName.map(_.value)},
         LASTNAME = ${staff.lastName.map(_.value)},
         BIRTHDATE = ${staff.birthDate.map(bd => java.sql.Date.valueOf(bd))},
         UPDATED_AT = ${Timestamp.valueOf(getCurrentTime)}
         WHERE ID = $staffId AND
         ORG_ID = ${staff.orgId} AND
         STATUS = ${StaffStatus.ACTIVE.entryName}
       """.update

  private def delete(orgId: UUID, staffId: UUID): Update0 =
    sql"""
          UPDATE STAFF SET
          STATUS = ${StaffStatus.INACTIVE.entryName} WHERE
          ID = $staffId AND
          ORG_ID = $orgId AND
          STATUS = ${StaffStatus.ACTIVE.entryName}
          """.update
}
