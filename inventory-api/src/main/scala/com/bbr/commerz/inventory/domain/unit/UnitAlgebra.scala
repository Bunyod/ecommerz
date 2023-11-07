package com.bbr.commerz.inventory.domain.unit

import com.bbr.commerz.inventory.domain.unit.UnitPayloads.{ProductUnit, UnitId, UnitName}
import com.bbr.platform.domain.Organization.OrgId

trait UnitAlgebra[F[_]] {
  def create(productUnit: ProductUnit): F[ProductUnit]
  def getByName(orgId: OrgId, unitName: UnitName): F[Option[ProductUnit]]
  def activateUnit(orgId: OrgId, unitName: UnitName): F[Unit]
  def updateById(unitId: UnitId, productUnit: ProductUnit): F[ProductUnit]
  def getAll(orgId: OrgId, name: Option[String]): F[List[ProductUnit]]
  def deleteById(orgId: OrgId, unitId: UnitId): F[String]
  def getById(orgId: OrgId, unitId: UnitId): F[ProductUnit]
}
