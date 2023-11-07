package com.bbr.commerz.inventory.infrastructure.postgres

import cats.effect._
import cats.implicits._
import com.bbr.commerz.inventory.domain.unit.UnitAlgebra
import com.bbr.commerz.inventory.domain.unit.UnitPayloads.{ProductUnit, UnitId, UnitStatus}
import com.bbr.commerz.inventory.domain.unit.UnitPayloads._
import com.bbr.platform.UuidOpts
import com.bbr.platform.domain.Organization.OrgId
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._ //DON'T REMOVE IT
import doobie.util.transactor.Transactor
import doobie.util.update.Update0

import java.util.UUID

class UnitRepository[F[_]: Async](tr: Transactor[F]) extends UnitAlgebra[F] {

  import UnitRepository._

  override def create(productUnit: ProductUnit): F[ProductUnit] =
    insert(productUnit).run.transact(tr) *> productUnit.pure[F]

  override def getByName(orgId: OrgId, unitName: UnitName): F[Option[ProductUnit]] =
    selectByName(orgId.value, unitName).option.transact(tr)

  override def activateUnit(orgId: OrgId, unitName: UnitName): F[Unit] =
    activate(orgId.value, unitName).run.transact(tr).void

  override def getById(orgId: OrgId, unitId: UnitId): F[ProductUnit] =
    selectById(orgId.value, unitId.value).option.transact(tr).flatMap {
      case Some(unit) => unit.pure[F]
      case None       => new Throwable(s"Could not find unit with this ID: ${unitId.value}").raiseError[F, ProductUnit]
    }

  override def updateById(unitId: UnitId, productUnit: ProductUnit): F[ProductUnit] =
    update(productUnit.orgId, unitId.value, productUnit.name).run.transact(tr) *> getById(productUnit.orgId, unitId)

  override def deleteById(orgId: OrgId, unitId: UnitId): F[String] =
    delete(orgId.value, unitId.value).run.transact(tr).flatMap {
      case 0 => new Throwable(s"Could not find unit with this ID: ${unitId.value}").raiseError[F, String]
      case _ => "Successfully deleted".pure[F]
    }

  override def getAll(
    orgId: OrgId,
    name: Option[String] = None
  ): F[List[ProductUnit]] =
    name match {
      case Some(n) => selectAll(orgId.value, n).to[List].transact(tr)
      case None    => selectAll(orgId.value).to[List].transact(tr)
    }
}

object UnitRepository {

  implicit val unitRead: Read[ProductUnit] =
    Read[
      (
        UUID,
        UUID,
        String,
        String
      )
    ]
      .map { case (id, orgId, name, status) =>
        ProductUnit(
          UnitId(id),
          orgId.toOrgId,
          UnitName(name),
          UnitStatus.withName(status)
        )
      }

  implicit val unitWrite: Write[ProductUnit] =
    Write[
      (
        UUID,
        UUID,
        String,
        String
      )
    ]
      .contramap { unit =>
        (
          unit.id.value,
          unit.orgId.value,
          unit.name.value,
          unit.status.entryName
        )
      }

  private def insert(productUnit: ProductUnit): Update0 =
    sql"""
          INSERT INTO UNIT (ID, ORG_ID, NAME, STATUS) VALUES (
          ${productUnit.id.value},
          ${productUnit.orgId.value},
          ${productUnit.name.value},
          ${productUnit.status.entryName}
          )""".update

  private def update(orgId: OrgId, unitId: UUID, unitName: UnitName): Update0 =
    sql"""
          UPDATE UNIT SET
          NAME = ${unitName.value}
          WHERE ID = $unitId AND ORG_ID = ${orgId.value}
          """.update

  private def selectAll(orgId: UUID, name: String): Query0[ProductUnit] =
    sql"""
          SELECT * FROM UNIT
          WHERE STATUS = ${UnitStatus.ACTIVE.entryName} AND
          NAME = $name AND
          ORG_ID = $orgId
          """.query

  private def delete(orgId: UUID, unitId: UUID): Update0 =
    sql"""
          UPDATE UNIT SET
          STATUS = ${UnitStatus.INACTIVE.entryName}
          WHERE ID = $unitId AND
          ORG_ID = $orgId AND
          STATUS = ${UnitStatus.ACTIVE.entryName}
          """.update

  private def selectById(orgId: UUID, unitId: UUID): Query0[ProductUnit] =
    sql"""SELECT * FROM UNIT WHERE ID = $unitId AND ORG_ID = $orgId AND STATUS = ${UnitStatus.ACTIVE.entryName}""".query

  private def selectByName(orgId: UUID, unitName: UnitName): Query0[ProductUnit] =
    sql"""SELECT * FROM UNIT WHERE NAME = ${unitName.value} AND ORG_ID = $orgId""".query

  private def activate(orgId: UUID, unitName: UnitName): Update0 =
    sql"""UPDATE UNIT SET
         STATUS = ${UnitStatus.ACTIVE.entryName}
         WHERE NAME = ${unitName.value} AND
         ORG_ID = $orgId
         """.update

  private def selectAll(orgId: UUID): Query0[ProductUnit] =
    sql"""
          SELECT * FROM UNIT
          WHERE STATUS = ${UnitStatus.ACTIVE.entryName} AND
          ORG_ID = $orgId
          """.query
}
