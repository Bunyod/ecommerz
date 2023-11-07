package com.bbr.commerz.inventory.domain.category

import CategoryPayloads._
import com.bbr.platform.domain.Organization.OrgId

trait CategoryAlgebra[F[_]] {

  def create(category: Category): F[Category]
  def getByName(orgId: OrgId, categoryName: CategoryName): F[Option[Category]]
  def activateCategory(orgId: OrgId, categoryName: CategoryName): F[Unit]
  def updateById(categoryId: CategoryId, category: Category): F[Category]
  def getAll(orgId: OrgId, name: Option[String] = None): F[List[Category]]
  def deleteById(orgId: OrgId, categoryId: CategoryId): F[String]
  def getById(orgId: OrgId, categoryId: CategoryId): F[Category]

}
