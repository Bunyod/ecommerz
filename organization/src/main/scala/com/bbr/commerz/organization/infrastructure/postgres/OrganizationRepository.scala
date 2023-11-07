package com.bbr.commerz.organization.infrastructure.postgres

import cats.effect.Async
import cats.implicits._
import com.bbr.commerz.organization.domain.organization.OrganizationAlgebra
import com.bbr.commerz.organization.domain.organization.OrganizationPayloads._
import com.bbr.commerz.organization.domain.owner.OwnerPayloads.Owner
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.PhoneNumber
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._             //DON'T REMOVE IT
import doobie.implicits.javasql._              //DON'T REMOVE IT
import doobie.postgres.circe.jsonb.implicits._ //DON'T REMOVE IT
import doobie.util.update.Update0
import io.circe.syntax._

import java.sql.Timestamp
import java.util.UUID

class OrganizationRepository[F[_]: Async](tr: Transactor[F]) extends OrganizationAlgebra[F] {

  import OrganizationRepository._

  override def create(organization: Organization, phoneNumber: PhoneNumber): F[Organization] =
    (for {
      _      <- insert(organization).run
      newOrg <- select(organization.id.value).unique
      orgs   <- selectOwnerOrgs(phoneNumber).unique.map(_.organizations)
      _      <- insertOrgToOwner(orgs :+ newOrg.id, phoneNumber).run
    } yield newOrg).transact(tr)

  override def getAll(limit: Option[Int], offset: Option[Int]): F[List[Organization]] =
    selectAll(limit.getOrElse(50), offset.getOrElse(0)).to[List].transact(tr)

  override def getById(id: OrgId): F[Organization] =
    select(id.value).option.transact(tr).flatMap {
      case Some(org) => org.pure[F]
      case None      => new Throwable(s"Organization not found with ID: $id").raiseError[F, Organization]
    }

  override def updateById(orgId: OrgId, organization: Organization): F[Organization] =
    update(orgId.value, organization).run.transact(tr) *> getById(organization.id)

  override def deleteById(id: OrgId): F[String] =
    delete(id.value).run.transact(tr).flatMap {
      case 0 => new Throwable(s"Could not find organization with this ID: $id").raiseError[F, String]
      case _ => "Successfully deleted".pure[F]
    }

  override def getAll(orgName: OrganizationName, limit: Int, offset: Int): F[List[Organization]] =
    selectOrganizations(orgName.name, limit, offset).to[List].transact(tr)

  override def checkOrganizationExistence(orgId: OrgId): F[Boolean] = checkOrganization(orgId.value).unique.transact(tr)
}

object OrganizationRepository {

  import Drivers._

  private def selectOwnerOrgs(phoneNumber: PhoneNumber): Query0[Owner] =
    sql"""SELECT * FROM OWNER WHERE PHONE_NUMBER = ${phoneNumber.value}""".query

  private def insertOrgToOwner(orgs: List[OrgId], phoneNumber: PhoneNumber): Update0 =
    sql"""UPDATE OWNER SET ORGANIZATIONS = ${orgs.asJson} WHERE PHONE_NUMBER = ${phoneNumber.value}""".update

  private def checkOrganization(orgId: UUID): Query0[Boolean] =
    sql"""SELECT EXISTS(SELECT * FROM ORGANIZATION WHERE ID = $orgId
         AND STATUS = ${OrganizationStatus.ACTIVE.entryName})""".query[Boolean]

  private def select(orgId: UUID): Query0[Organization] =
    sql"""SELECT * FROM ORGANIZATION WHERE ID = $orgId""".query

  private def selectAll(limit: Int, offset: Int): Query0[Organization] =
    sql"""
          SELECT * FROM ORGANIZATION
          LIMIT $limit
          OFFSET $offset
       """.query

  private def insert(organization: Organization): Update0 =
    sql"""
      INSERT INTO ORGANIZATION (ID, NAME, STATUS, CREATED_AT, UPDATED_AT)
      VALUES (
        ${organization.id.value},
        ${organization.name.name},
        ${organization.status.entryName},
        ${organization.createdAt.map(Timestamp.valueOf)},
        ${organization.updatedAt.map(Timestamp.valueOf)}
      )
       """.update

  private def selectOrganizations(orgName: String, limit: Int, offset: Int): Query0[Organization] =
    sql"""
         SELECT * FROM ORGANIZATION
         WHERE NAME = $orgName
         LIMIT $limit
         OFFSET $offset
         """.query

  private def update(id: UUID, organization: Organization): Update0 =
    sql"""
         UPDATE ORGANIZATION SET NAME = ${organization.name.name},
         UPDATED_AT = ${organization.updatedAt.map(Timestamp.valueOf)}
         WHERE ID = $id
       """.update

  private def delete(orgId: UUID): Update0 =
    sql"""
         UPDATE ORGANIZATION SET
         STATUS = ${OrganizationStatus.INACTIVE.entryName} WHERE
         ID = $orgId AND
         STATUS = ${OrganizationStatus.ACTIVE.entryName}
       """.update

}
