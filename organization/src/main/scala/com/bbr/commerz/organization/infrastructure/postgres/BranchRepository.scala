package com.bbr.commerz.organization.infrastructure.postgres

import cats.effect.Async
import cats.implicits._
import com.bbr.commerz.organization.domain.branch.BranchAlgebra
import com.bbr.commerz.organization.domain.branch.BranchPayloads._
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.commerz.organization.http.utils.json._
import com.bbr.platform.domain.Branch.BranchId
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._             // DON'T REMOVE IT
import doobie.implicits.javasql._              // DON'T REMOVE IT
import doobie.postgres.circe.jsonb.implicits._ // DON'T REMOVE IT
import doobie.util.update.Update0
import io.circe.syntax._

import java.sql.Timestamp
import java.util.UUID

class BranchRepository[F[_]: Async](tr: Transactor[F]) extends BranchAlgebra[F] {
  import BranchRepository._

  override def create(branch: Branch): F[Branch] =
    insert(branch).run.transact(tr) *> getById(branch.orgId, branch.id)

  override def getByName(branchName: BranchName, orgId: OrgId): F[Option[Branch]] =
    selectByName(branchName, orgId).option.transact(tr).flatMap(_.traverse(Async[F].fromEither))

  override def activateBranch(orgId: OrgId, branchName: BranchName): F[Unit] =
    activate(branchName, orgId).run.transact(tr).void

  override def getAll(
    orgId: OrgId,
    branchName: Option[BranchName]
  ): F[List[Branch]] =
    (branchName match {
      case Some(name) => selectAll(orgId.value, name.value).to[List].transact(tr)
      case None       => selectAll(orgId.value).to[List].transact(tr)
    }).flatMap(_.traverse(Async[F].fromEither))

  override def updateById(branchId: BranchId, branch: Branch): F[Branch] =
    update(branchId.value, branch).run.transact(tr) *> getById(branch.orgId, branchId)

  override def getById(orgId: OrgId, branchId: BranchId): F[Branch] =
    select(orgId.value, branchId.value).option.transact(tr).flatMap {
      case Some(branch) => Async[F].fromEither(branch)
      case None         => new Throwable(s"Couldn't find branch with this id: ${branchId.value}").raiseError[F, Branch]
    }

  override def deleteById(orgId: OrgId, branchId: BranchId): F[String] =
    delete(orgId.value, branchId.value).run.transact(tr).flatMap {
      case 0 => new Throwable(s"Could not find branch with this ID: ${branchId.value}").raiseError[F, String]
      case _ => "Successfully deleted".pure[F]
    }

  override def checkBranchExistence(branchId: BranchId): F[Boolean] = checkBranch(branchId.value).unique.transact(tr)
}

object BranchRepository {

  import Drivers._

  private def checkBranch(branchId: UUID): Query0[Boolean] =
    sql"""SELECT EXISTS(SELECT * FROM BRANCH WHERE ID = $branchId
         AND STATUS = ${BranchStatus.ACTIVE.entryName})""".query

  private def select(orgId: UUID, branchId: UUID): Query0[Either[Throwable, Branch]] =
    sql"""
          SELECT ID, NAME, ORG_ID, ADDRESS, STATUS, CREATED_BY, CREATED_AT
          FROM BRANCH
          WHERE ID = $branchId AND
          ORG_ID = $orgId AND
          STATUS = ${BranchStatus.ACTIVE.entryName}
          """.query

  private def selectAll(orgId: UUID, name: String): Query0[Either[Throwable, Branch]] =
    sql"""
          SELECT ID, NAME, ORG_ID, ADDRESS, STATUS, CREATED_BY, CREATED_AT
          FROM BRANCH
          WHERE ORG_ID = $orgId AND
          NAME = $name
          """.query

  private def selectAll(orgId: UUID): Query0[Either[Throwable, Branch]] =
    sql"""
          SELECT ID, NAME, ORG_ID, ADDRESS, STATUS, CREATED_BY, CREATED_AT
          FROM BRANCH
          WHERE ORG_ID = $orgId
          """.query

  private def selectByName(branchName: BranchName, orgId: OrgId): Query0[Either[Throwable, Branch]] =
    sql"""
          SELECT ID, NAME, ORG_ID, ADDRESS, STATUS, CREATED_BY, CREATED_AT
          FROM BRANCH
          WHERE NAME = ${branchName.value} AND
          ORG_ID = ${orgId.value}
          """.query

  private def activate(branchName: BranchName, orgId: OrgId): Update0 =
    sql"""
          UPDATE BRANCH SET
          STATUS = ${BranchStatus.ACTIVE.entryName}
          WHERE NAME = ${branchName.value} AND
          ORG_ID = ${orgId.value}
          """.update

  private def insert(branch: Branch): Update0 =
    sql"""
      INSERT INTO BRANCH (ID, ORG_ID, NAME, ADDRESS, STATUS, CREATED_BY, CREATED_AT)
      VALUES (
        ${branch.id.value},
        ${branch.orgId.value},
        ${branch.branchName.value},
        ${branch.address.asJson},
        ${branch.status.entryName},
        ${branch.createdBy.value},
        ${branch.createdAt.map(Timestamp.valueOf)}
      )
       """.update

  private def delete(orgId: UUID, branchId: UUID): Update0 =
    sql"""
         UPDATE BRANCH SET
         STATUS = ${BranchStatus.INACTIVE.entryName} WHERE
         ID = $branchId AND
         ORG_ID = $orgId AND
         STATUS = ${BranchStatus.ACTIVE.entryName}
       """.update

  private def update(
    branchId: UUID,
    branch: Branch
  ): Update0 =
    sql"""
         UPDATE BRANCH SET
         NAME = ${branch.branchName.value},
         ADDRESS = ${branch.address.asJson}
         WHERE ID = $branchId AND
         ORG_ID = ${branch.orgId.value}
       """.update
}
