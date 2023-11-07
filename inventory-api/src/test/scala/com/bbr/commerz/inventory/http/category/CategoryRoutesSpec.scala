package com.bbr.commerz.inventory.http.category

import cats.effect._
import cats.implicits._
import com.bbr.commerz.inventory.domain.category.{CategoryAlgebra, CategoryService}
import com.bbr.commerz.inventory.domain.category.CategoryPayloads._
import com.bbr.commerz.inventory.suite.InventoryGenerators
import com.bbr.commerz.inventory.suite.json._
import com.bbr.commerz.inventory.suite.json.showCategory
import com.bbr.commerz.organization.suite.HttpTestSuite
import com.bbr.commerz.inventory.http._
import com.bbr.platform.domain.Organization.OrgId
import io.circe.syntax._
import org.http4s.Method._
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.headers.`Content-Type`

import java.util.UUID

object CategoryRoutesSpec extends HttpTestSuite with InventoryGenerators {

  private def createCategory(newCategory: Category): CategoryService[IO] =
    new CategoryService[IO](new TestCategoryRepository {
      override def create(category: Category): IO[Category] = newCategory.pure[IO]
    })

  private def failingCreateCategory(): CategoryService[IO] =
    new CategoryService[IO](new TestCategoryRepository {
      override def create(category: Category): IO[Category] =
        IO.raiseError(DummyError) *> IO.pure(category)
    })

  private def deleteById(): CategoryService[IO] =
    new CategoryService[IO](new TestCategoryRepository {
      override def deleteById(orgId: OrgId, category: CategoryId): IO[String] = IO.pure("Successfully deleted")
    })

  private def failingDeleteById(): CategoryService[IO] =
    new CategoryService[IO](new TestCategoryRepository {
      override def deleteById(orgId: OrgId, id: CategoryId): IO[String] =
        IO.raiseError(DummyError) *> IO.pure("Deleted")
    })

  private def getByParams(categories: List[Category]): CategoryService[IO] =
    new CategoryService[IO](new TestCategoryRepository {
      override def getAll(orgId: OrgId, name: Option[String]): IO[List[Category]] =
        IO.pure(categories)
    })

  private def failingGetByParam(): CategoryService[IO] = new CategoryService[IO](new TestCategoryRepository {
    override def getAll(orgId: OrgId, name: Option[String]): IO[List[Category]] =
      IO.raiseError(DummyError) *> IO.pure(List.empty)
  })

  private def getById(category: Category): CategoryService[IO] =
    new CategoryService[IO](new TestCategoryRepository {
      override def getById(orgId: OrgId, categoryId: CategoryId): IO[Category] = IO.pure(category)
    })

  private def failingGetById(category: Category): CategoryService[IO] =
    new CategoryService[IO](new TestCategoryRepository {
      override def getById(orgId: OrgId, categoryId: CategoryId): IO[Category] =
        IO.raiseError(DummyError) *> IO.pure(category)
    })

  private def updateById(newCategory: Category): CategoryService[IO] =
    new CategoryService[IO](new TestCategoryRepository {
      override def updateById(categoryId: CategoryId, category: Category): IO[Category] = newCategory.pure[IO]
    })

  private def failingUpdateById(): CategoryService[IO] =
    new CategoryService[IO](new TestCategoryRepository {
      override def updateById(categoryId: CategoryId, category: Category): IO[Category] =
        IO.raiseError(DummyError) *> IO.pure(category)
    })

  test("POST create category [CREATED]") {
    forall(genCategory) { category =>
      val categoryRequest        = genCategoryRequest.sample.get
      val req                    = POST(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/categories"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(categoryRequest.asJson)
      val routes: HttpRoutes[IO] =
        new CategoryRoutes[IO](createCategory(category)).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpBodyAndStatus(routes, req)(category, Status.Created)
    }
  }
  test("POST create category [FAILURE]") {
    forall(genCategoryRequest) { categoryRequest =>
      val req                    = POST(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/categories"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(categoryRequest.asJson)
      val routes: HttpRoutes[IO] =
        new CategoryRoutes[IO](failingCreateCategory()).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpFailure(routes, req)
    }
  }
  test("POST create category wrong JSON [FAILURE]") {
    forall(genOrgId) { orgId =>
      val req                    = POST(Uri.unsafeFromString(s"/org/${orgId.value}/inventory/categories"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity("Wrong")
      val routes: HttpRoutes[IO] =
        new CategoryRoutes[IO](failingCreateCategory()).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpFailure(routes, req)
    }
  }
  test("POST create category bad request [FAILURE]") {
    forall(genCategory) { category =>
      val req                    = POST(Uri.unsafeFromString(s"/wrong/categories"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(category.asJson)
      val routes: HttpRoutes[IO] =
        new CategoryRoutes[IO](failingCreateCategory()).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpFailure(routes, req)
    }
  }

  test("DELETE category [OK]") {
    forall(genOrgId) { orgId =>
      val req = DELETE(Uri.unsafeFromString(s"/org/${orgId.value}/inventory/categories/${UUID.randomUUID()}"))

      val routes: HttpRoutes[IO] = new CategoryRoutes[IO](deleteById()).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpStatus(routes, req)(Status.Ok)
    }
  }
  test("DELETE category [FAILURE]") {
    forall(genOrgId) { orgId =>
      val req = DELETE(Uri.unsafeFromString(s"/org/${orgId.value}/inventory/categories/${UUID.randomUUID()}"))

      val routes: HttpRoutes[IO] =
        new CategoryRoutes[IO](failingDeleteById()).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpFailure(routes, req)
    }
  }
  test("DELETE category bad request [FAILURE]") {
    forall(genOrgId) { id =>
      val req                    = DELETE(Uri.unsafeFromString(s"/wrong/${id.value}"))
      val routes: HttpRoutes[IO] =
        new CategoryRoutes[IO](failingDeleteById()).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpFailure(routes, req)
    }
  }

  test("GET categories by params [OK]") {
    forall(genCategories) { categories =>
      val req                    = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/categories"))
      val routes: HttpRoutes[IO] =
        new CategoryRoutes[IO](getByParams(categories)).routes(authMiddleware(staffAuthWithAgent))
      expectHttpBodyAndStatus(routes, req)(categories, Status.Ok)
    }
  }
  test("GET categories by params [FAILURE]") {
    forall(genOrgId) { orgId =>
      val req                    = GET(Uri.unsafeFromString(s"/org/${orgId.value}/inventory/categories"))
      val routes: HttpRoutes[IO] =
        new CategoryRoutes[IO](failingGetByParam()).routes(authMiddleware(staffAuthWithAgent))
      expectHttpFailure(routes, req)
    }
  }
  test("GET categories by params bad request [FAILURE]") {
    forall(genOrgId) { orgId =>
      val req                    = GET(Uri.unsafeFromString(s"/org/${orgId.value}/inventory/categories"))
      val routes: HttpRoutes[IO] =
        new CategoryRoutes[IO](failingGetByParam()).routes(authMiddleware(staffAuthWithAgent))
      expectHttpFailure(routes, req)
    }
  }

  test("GET category by id [OK]") {
    forall(genCategory) { category =>
      val req                    = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/categories"))
      val routes: HttpRoutes[IO] =
        new CategoryRoutes[IO](getById(category)).routes(authMiddleware(staffAuthWithAgent))
      expectHttpStatus(routes, req)(Status.Ok)
    }
  }
  test("GET category by id [FAILURE]") {
    forall(genCategory) { category =>
      val req = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/categories/${UUID.randomUUID()}"))

      val routes: HttpRoutes[IO] =
        new CategoryRoutes[IO](failingGetById(category)).routes(authMiddleware(staffAuthWithAgent))
      expectHttpFailure(routes, req)
    }
  }
  test("GET category by id bad request [FAILURE]") {
    forall(genCategory) { category =>
      val req = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/categories/${UUID.randomUUID()}"))

      val routes: HttpRoutes[IO] =
        new CategoryRoutes[IO](failingGetById(category)).routes(authMiddleware(staffAuthWithAgent))
      expectHttpFailure(routes, req)
    }
  }

  test("PUT update category [OK]") {
    forall(genCategory) { category =>
      val categoryRequest        = genCategoryRequest.sample.get
      val req                    =
        PUT(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/categories/${UUID.randomUUID()}"))
          .withContentType(`Content-Type`(MediaType.application.json))
          .withEntity(categoryRequest.asJson)
      val routes: HttpRoutes[IO] =
        new CategoryRoutes[IO](updateById(category)).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpBodyAndStatus(routes, req)(category, Status.Ok)
    }
  }
  test("PUT update category [FAILURE]") {
    forall(genCategoryRequest) { request =>
      val req =
        PUT(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/categories/${UUID.randomUUID()}"))
          .withContentType(`Content-Type`(MediaType.application.json))
          .withEntity(request.asJson)

      val routes: HttpRoutes[IO] =
        new CategoryRoutes[IO](failingUpdateById()).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpFailure(routes, req)
    }
  }
  test("PUT update category wrong JSON [FAILURE]") {
    forall(genOrgId) { orgId =>
      val req                    = PUT(Uri.unsafeFromString(s"/wrong/inventory/${orgId.value}/categories/${UUID.randomUUID()}"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity("")
      val routes: HttpRoutes[IO] =
        new CategoryRoutes[IO](failingUpdateById()).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpFailure(routes, req)
    }
  }

  test("PUT update category bad request [FAILURE]") {
    forall(genCategory) { category =>
      val req                    = PUT(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/categories/${UUID.randomUUID()}"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(category.asJson)
      val routes: HttpRoutes[IO] =
        new CategoryRoutes[IO](failingUpdateById()).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpFailure(routes, req)
    }
  }
}

protected class TestCategoryRepository extends CategoryAlgebra[IO] {

  override def getByName(orgId: OrgId, categoryName: CategoryName): IO[Option[Category]] = IO.pure(None)

  override def activateCategory(orgId: OrgId, categoryName: CategoryName): IO[Unit] = IO.pure(())

  override def create(category: Category): IO[Category]                    =
    category.pure[IO]
  override def getById(orgId: OrgId, categoryId: CategoryId): IO[Category] = IO.pure(null)

  override def updateById(categoryId: CategoryId, category: Category): IO[Category] = category.pure[IO]

  override def deleteById(orgId: OrgId, category: CategoryId): IO[String] =
    IO.pure("deleted")

  override def getAll(orgId: OrgId, name: Option[String]): IO[List[Category]] =
    IO.pure(List.empty)

}
