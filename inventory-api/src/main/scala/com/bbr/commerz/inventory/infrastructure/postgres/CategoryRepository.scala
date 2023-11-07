package com.bbr.commerz.inventory.infrastructure.postgres

import cats.effect._
import cats.implicits._
import com.bbr.commerz.inventory.domain.category.CategoryAlgebra
import com.bbr.commerz.inventory.domain.category.CategoryPayloads._
import com.bbr.platform.UuidOpts
import com.bbr.platform.domain.Organization.OrgId
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._ // DON'T REMOVE IT
import doobie.util.transactor.Transactor
import doobie.util.update.Update0

import java.util.UUID

class CategoryRepository[F[_]: Async](tr: Transactor[F]) extends CategoryAlgebra[F] {

  import CategoryRepository._

  override def create(category: Category): F[Category] =
    insert(category).run.transact(tr) *> getById(category.orgId, category.id)

  override def getByName(orgId: OrgId, categoryName: CategoryName): F[Option[Category]] =
    selectByName(orgId.value, categoryName).option.transact(tr)

  override def activateCategory(orgId: OrgId, categoryName: CategoryName): F[Unit] =
    activate(orgId.value, categoryName).run.transact(tr).void

  override def getById(orgId: OrgId, categoryId: CategoryId): F[Category] =
    selectById(orgId.value, categoryId.value).option.transact(tr).flatMap {
      case Some(category) => category.pure[F]
      case None           => new Throwable(s"Could not find category with this ID: ${categoryId.value}").raiseError[F, Category]
    }

  override def updateById(categoryId: CategoryId, category: Category): F[Category] =
    update(categoryId.value, category).run.transact(tr) *> getById(category.orgId, categoryId)

  override def deleteById(orgId: OrgId, categoryId: CategoryId): F[String] =
    delete(orgId.value, categoryId.value).run.transact(tr).flatMap {
      case 0 => new Throwable(s"Could not find category with this ID: ${categoryId.value}").raiseError[F, String]
      case _ => "Successfully deleted".pure[F]
    }

  override def getAll(
    orgId: OrgId,
    name: Option[String] = None
  ): F[List[Category]] =
    name match {
      case Some(n) => selectAll(orgId.value, n).to[List].transact(tr)
      case None    => selectAll(orgId.value).to[List].transact(tr)
    }
}

object CategoryRepository {

  implicit val categoryRead: Read[Category] =
    Read[
      (
        UUID,
        UUID,
        String,
        String
      )
    ]
      .map { case (id, orgId, name, status) =>
        Category(
          CategoryId(id),
          orgId.toOrgId,
          CategoryName(name),
          CategoryStatus.withName(status)
        )
      }

  implicit val categoryWrite: Write[Category] =
    Write[
      (
        UUID,
        UUID,
        String,
        String
      )
    ]
      .contramap { category =>
        (
          category.id.value,
          category.orgId.value,
          category.name.value,
          category.status.entryName
        )
      }

  private def insert(category: Category): Update0 =
    sql"""INSERT INTO CATEGORY (ID, ORG_ID, NAME, STATUS) VALUES (
          ${category.id.value},
          ${category.orgId.value},
          ${category.name.value},
          ${category.status.entryName}
          )""".update

  private def update(categoryId: UUID, category: Category): Update0 =
    sql"""
          UPDATE CATEGORY SET
          NAME = ${category.name.value}
          WHERE ID = $categoryId AND ORG_ID = ${category.orgId.value}
          """.update

  private def selectAll(orgId: UUID, name: String): Query0[Category] =
    sql"""
          SELECT * FROM CATEGORY
          WHERE STATUS = ${CategoryStatus.ACTIVE.entryName} AND
          NAME = $name AND
          ORG_ID = $orgId
         """.query

  private def delete(orgId: UUID, categoryId: UUID): Update0 =
    sql"""
          UPDATE CATEGORY SET
          STATUS = ${CategoryStatus.INACTIVE.entryName}
          WHERE ID = $categoryId AND
          ORG_ID = $orgId AND
          STATUS = ${CategoryStatus.ACTIVE.entryName}
          """.update

  private def selectById(orgId: UUID, categoryId: UUID): Query0[Category] =
    sql"""SELECT * FROM CATEGORY
         WHERE ID = $categoryId AND
         ORG_ID = $orgId AND
         STATUS = ${CategoryStatus.ACTIVE.entryName}
         """.query

  private def selectByName(orgId: UUID, categoryName: CategoryName): Query0[Category] =
    sql"""SELECT * FROM CATEGORY WHERE NAME = ${categoryName.value} AND ORG_ID = $orgId""".query

  private def activate(orgId: UUID, categoryName: CategoryName): Update0 =
    sql"""UPDATE CATEGORY SET
         STATUS = ${CategoryStatus.ACTIVE.entryName}
         WHERE NAME = ${categoryName.value} AND
         ORG_ID = $orgId
         """.update

  private def selectAll(orgId: UUID): Query0[Category] =
    sql"""
          SELECT * FROM CATEGORY
          WHERE STATUS = ${CategoryStatus.ACTIVE.entryName} AND
          ORG_ID = $orgId
          """.query

}
