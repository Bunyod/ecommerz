package com.bbr.commerz.inventory.http.unit

import cats.effect.IO
import cats.implicits._
import com.bbr.commerz.inventory.domain.unit.UnitPayloads._
import com.bbr.commerz.inventory.domain.unit.{UnitAlgebra, UnitService}
import com.bbr.commerz.inventory.suite.InventoryGenerators
import com.bbr.commerz.inventory.suite.json._
import com.bbr.commerz.organization.suite.HttpTestSuite
import com.bbr.commerz.organization.suite.json.showBranch
import com.bbr.commerz.inventory.http._
import com.bbr.platform.domain.Organization.OrgId
import io.circe.syntax._
import org.http4s.Method._
import org.http4s._
import org.http4s.circe.jsonEncoder
import org.http4s.client.dsl.io._
import org.http4s.headers.`Content-Type`

import java.util.UUID

object UnitRoutesSpec extends HttpTestSuite with InventoryGenerators {

  def createUnit(newUnit: ProductUnit): UnitService[IO] = new UnitService[IO](new TestUnitRepository {
    override def create(productUnit: ProductUnit): IO[ProductUnit] =
      ProductUnit(id = newUnit.id, orgId = newUnit.orgId, name = newUnit.name, status = newUnit.status).pure[IO]
  })

  def failingCreateUnit(): UnitService[IO] =
    new UnitService[IO](new TestUnitRepository {
      override def create(productUnit: ProductUnit): IO[ProductUnit] =
        IO.raiseError(DummyError) *> productUnit.pure[IO]
    })

  def deleteById(): UnitService[IO] =
    new UnitService[IO](new TestUnitRepository {
      override def deleteById(orgId: OrgId, unitId: UnitId): IO[String] = IO.pure("Successfully deleted")
    })

  def failingDeleteById(): UnitService[IO] =
    new UnitService[IO](new TestUnitRepository {
      override def deleteById(orgId: OrgId, unitId: UnitId): IO[String] =
        IO.raiseError(DummyError)
    })

  def getByParams(productUnits: List[ProductUnit]): UnitService[IO] = new UnitService[IO](new TestUnitRepository {
    override def getAll(orgId: OrgId, name: Option[String]): IO[List[ProductUnit]] =
      IO.pure(productUnits)
  })

  def failingGetByParam(): UnitService[IO] =
    new UnitService[IO](new TestUnitRepository {
      override def getAll(orgId: OrgId, name: Option[String]): IO[List[ProductUnit]] =
        IO.raiseError(DummyError) *> IO.pure(List.empty[ProductUnit])
    })

  def getById(productUnit: ProductUnit): UnitService[IO] =
    new UnitService[IO](new TestUnitRepository {
      override def getById(orgId: OrgId, unitId: UnitId): IO[ProductUnit] =
        IO.pure(productUnit)
    })

  def failingGetById(): UnitService[IO] =
    new UnitService[IO](new TestUnitRepository {
      override def getById(orgId: OrgId, unitId: UnitId): IO[ProductUnit] =
        IO.raiseError(DummyError) *> IO.pure(genProductUnit.sample.get)
    })

  def updateById(newUnit: ProductUnit): UnitService[IO] = new UnitService[IO](new TestUnitRepository {
    override def updateById(unitId: UnitId, productUnit: ProductUnit): IO[ProductUnit] =
      ProductUnit(id = newUnit.id, orgId = newUnit.orgId, name = newUnit.name, status = newUnit.status).pure[IO]
  })

  def failingUpdateById(): UnitService[IO] = new UnitService[IO](new TestUnitRepository {
    override def updateById(unitId: UnitId, productUnit: ProductUnit): IO[ProductUnit] =
      IO.raiseError(DummyError) *> IO.pure(productUnit)
  })

  def getUnitsByParam(units: List[ProductUnit]): UnitService[IO] = new UnitService[IO](new TestUnitRepository {
    override def getAll(orgId: OrgId, name: Option[String]): IO[List[ProductUnit]] =
      units.pure[IO]
  })

  def failingGetUnitsByParam(): UnitService[IO] = new UnitService[IO](new TestUnitRepository {
    override def getAll(orgId: OrgId, name: Option[String]): IO[List[ProductUnit]] =
      IO.raiseError(DummyError)
  })

  test("POST create unit [CREATED]") {
    forall(genProductUnit) { productUnit =>
      val unitRequest            = genUnitRequest.sample.get
      val req                    = POST(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/units"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(unitRequest.asJson)
      val routes: HttpRoutes[IO] =
        new UnitRoutes[IO](createUnit(productUnit)).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpBodyAndStatus(routes, req)(productUnit, Status.Created)
    }
  }
  test("POST create unit [FAILURE]")(
    forall(genUnitRequest) { request =>
      val req                    = POST(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/units"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(request.asJson)
      val routes: HttpRoutes[IO] = new UnitRoutes[IO](failingCreateUnit()).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpFailure(routes, req)
    }
  )
  test("POST create unit wrong JSON [FAILURE]")(
    forall(genUnitRequest) { request =>
      val req                    = POST(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/units"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(s"Wrong: $request")
      val routes: HttpRoutes[IO] = new UnitRoutes[IO](failingCreateUnit()).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpFailure(routes, req)
    }
  )
  test("POST create unit bad request [FAILURE]")(
    forall(genUnitRequest) { request =>
      val req                    = POST(Uri.unsafeFromString(s"/wrong123"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(request.asJson)
      val routes: HttpRoutes[IO] = new UnitRoutes[IO](failingCreateUnit()).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpFailure(routes, req)
    }
  )

  test("GET units [OK]")(
    forall(genProductUnits) { productUnits =>
      val req                    = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/units"))
      val routes: HttpRoutes[IO] =
        new UnitRoutes[IO](getUnitsByParam(productUnits)).routes(authMiddleware(staffAuthWithAgent))
      expectHttpBodyAndStatus(routes, req)(productUnits, Status.Ok)
    }
  )
  test("GET units [FAILURE]")(
    forall(genBranches) { _ =>
      val req                    = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/units"))
      val routes: HttpRoutes[IO] =
        new UnitRoutes[IO](failingGetUnitsByParam()).routes(authMiddleware(staffAuthWithAgent))
      expectHttpFailure(routes, req)
    }
  )
  test("GET units bad request [FAILURE]")(
    forall(genBranches) { _ =>
      val req                    = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/units"))
      val routes: HttpRoutes[IO] =
        new UnitRoutes[IO](failingGetUnitsByParam()).routes(authMiddleware(staffAuthWithAgent))
      expectHttpFailure(routes, req)
    }
  )

  test("GET unit by id [OK]")(
    forall(genProductUnit) { productUnit =>
      val req = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/units/${UUID.randomUUID()}"))

      val routes: HttpRoutes[IO] = new UnitRoutes[IO](getById(productUnit)).routes(authMiddleware(staffAuthWithAgent))
      expectHttpBodyAndStatus(routes, req)(productUnit, Status.Ok)
    }
  )
  test("GET unit by id [FAILURE]")(
    forall(genOrgId) { orgId =>
      val req = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/units/${orgId.value}"))

      val routes: HttpRoutes[IO] =
        new UnitRoutes[IO](failingGetById()).routes(authMiddleware(staffAuthWithAgent))
      expectHttpFailure(routes, req)
    }
  )
  test("GET unit by id bad request [FAILURE]")(
    forall(genOrgId) { orgId =>
      val req = GET(Uri.unsafeFromString(s"/wrong123/${orgId.value}"))

      val routes: HttpRoutes[IO] =
        new UnitRoutes[IO](failingGetById()).routes(authMiddleware(staffAuthWithAgent))
      expectHttpFailure(routes, req)
    }
  )

  test("PUT update unit [OK]")(
    forall(genProductUnit) { productUnit =>
      val unitRequest = genUnitRequest.sample.get
      val req         = PUT(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/units/${UUID.randomUUID()}"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(unitRequest.asJson)

      val routes: HttpRoutes[IO] =
        new UnitRoutes[IO](updateById(productUnit)).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpBodyAndStatus(routes, req)(productUnit, Status.Ok)
    }
  )
  test("PUT update unit [FAILURE]")(
    forall(genUnitRequest) { request =>
      val req = PUT(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/units/${UUID.randomUUID()}"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(request.asJson)

      val routes: HttpRoutes[IO] =
        new UnitRoutes[IO](failingUpdateById()).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpFailure(routes, req)
    }
  )
  test("PUT update unit bad request [FAILURE]")(
    forall(genUnitRequest) { request =>
      val req                    = PUT(Uri.unsafeFromString(s"/wrong123"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(request.asJson)
      val routes: HttpRoutes[IO] = new UnitRoutes[IO](failingUpdateById()).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpFailure(routes, req)
    }
  )
  test("PUT create unit wrong JSON [FAILURE]")(
    forall(genUnitRequest) { request =>
      val req = PUT(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/units/${UUID.randomUUID()}"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(s"Wrong: $request")

      val routes: HttpRoutes[IO] = new UnitRoutes[IO](failingCreateUnit()).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpFailure(routes, req)
    }
  )

  test("DELETE unit [OK]")(
    forall(genOrgId) { orgId =>
      val req = DELETE(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/units/${orgId.value}"))

      val routes: HttpRoutes[IO] = new UnitRoutes[IO](deleteById()).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpStatus(routes, req)(Status.Ok)
    }
  )
  test("DELETE branch [FAILURE]")(
    forall(genOrgId) { orgId =>
      val req = DELETE(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/units/${orgId.value}"))

      val routes: HttpRoutes[IO] = new UnitRoutes[IO](failingDeleteById()).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpFailure(routes, req)
    }
  )
  test("DELETE unit bad request [FAILURE]")(
    forall(genOrgId) { id =>
      val req = DELETE(Uri.unsafeFromString(s"/wrong123/${id.value}"))

      val routes: HttpRoutes[IO] = new UnitRoutes[IO](failingDeleteById()).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpFailure(routes, req)
    }
  )
}

protected class TestUnitRepository extends UnitAlgebra[IO] {

  override def getByName(orgId: OrgId, unitName: UnitName): IO[Option[ProductUnit]] = IO.pure(None)

  override def activateUnit(orgId: OrgId, unitName: UnitName): IO[Unit] = IO.pure(())

  override def create(productUnit: ProductUnit): IO[ProductUnit] = IO.pure(productUnit)

  override def updateById(unitId: UnitId, productUnit: ProductUnit): IO[ProductUnit] =
    IO.pure(productUnit)

  override def getAll(orgId: OrgId, name: Option[String]): IO[List[ProductUnit]] =
    IO.pure(List.empty[ProductUnit])

  override def deleteById(orgId: OrgId, unitId: UnitId): IO[String] = IO.pure("Deleted successfully")

  override def getById(orgId: OrgId, unitId: UnitId): IO[ProductUnit] = IO.pure(null)
}
