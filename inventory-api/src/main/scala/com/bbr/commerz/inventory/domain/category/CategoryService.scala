package com.bbr.commerz.inventory.domain.category

import cats._
import cats.implicits._
import CategoryPayloads._
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.effekts.GenUUID

class CategoryService[F[_]: GenUUID: MonadThrow](categoryAlgebra: CategoryAlgebra[F]) {

  def create(orgId: OrgId, category: CategoryRequest): F[Category] =
    for {
      uuid <- GenUUID[F].make
      ctg  <- categoryAlgebra.getByName(orgId, category.name.toDomain)
      res  <-
        ctg match {
          case None                                           =>
            categoryAlgebra.create(category.toDomain(orgId, CategoryId(uuid)))
          case Some(c) if c.status == CategoryStatus.INACTIVE =>
            categoryAlgebra.activateCategory(orgId, c.name) *> c.copy(status = CategoryStatus.ACTIVE).pure[F]
          case _                                              =>
            new Throwable(s"The category already exists: ${category.name.value.value}").raiseError[F, Category]
        }
    } yield res

  def updateById(orgId: OrgId, categoryId: CategoryId, category: CategoryRequest): F[Category] =
    categoryAlgebra.updateById(categoryId, category.toDomain(orgId, categoryId))

  def getById(orgId: OrgId, categoryId: CategoryId): F[Category] =
    categoryAlgebra.getById(orgId, categoryId)

  def deleteById(orgId: OrgId, categoryId: CategoryId): F[String] =
    categoryAlgebra.deleteById(orgId, categoryId)

  def getAll(
    orgId: OrgId,
    name: Option[String] = None
  ): F[List[Category]] =
    categoryAlgebra.getAll(orgId, name)
}
