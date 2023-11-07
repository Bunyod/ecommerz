package com.bbr.commerz.inventory.domain.unit

import cats._
import cats.implicits._
import com.bbr.commerz.inventory.domain.unit.UnitPayloads.{ProductUnit, UnitId, UnitRequest, UnitStatus}
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.effekts.GenUUID

class UnitService[F[_]: GenUUID: MonadThrow](unitAlgebra: UnitAlgebra[F]) {

  def create(orgId: OrgId, productUnit: UnitRequest): F[ProductUnit] =
    for {
      uuid <- GenUUID[F].make
      unit <- unitAlgebra.getByName(orgId, productUnit.name.toDomain)
      res  <- unit match {
                case None                                       =>
                  unitAlgebra.create(productUnit.toDomain(orgId, UnitId(uuid)))
                case Some(u) if u.status == UnitStatus.INACTIVE =>
                  unitAlgebra.activateUnit(orgId, u.name) *> u.copy(status = UnitStatus.ACTIVE).pure[F]
                case _                                          =>
                  new Throwable(s"The unit already exists: ${productUnit.name.value.value}").raiseError[F, ProductUnit]
              }
    } yield res

  def updateById(orgId: OrgId, unitId: UnitId, unit: UnitRequest): F[ProductUnit] =
    unitAlgebra.updateById(unitId, unit.toDomain(orgId, unitId))

  def getById(orgId: OrgId, unitId: UnitId): F[ProductUnit] =
    unitAlgebra.getById(orgId, unitId)

  def deleteById(orgId: OrgId, unitId: UnitId): F[String] =
    unitAlgebra.deleteById(orgId, unitId)

  def getAll(
    orgId: OrgId,
    name: Option[String] = None
  ): F[List[ProductUnit]] =
    unitAlgebra.getAll(orgId, name)
}
