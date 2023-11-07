package com.bbr.commerz.organization.http.organization

import cats.effect._
import cats.implicits._
import com.bbr.commerz.organization.domain.organization.{OrganizationAlgebra, OrganizationService}
import com.bbr.commerz.organization.domain.organization.OrganizationPayloads._
import com.bbr.commerz.organization.http.authMiddleware
import com.bbr.commerz.organization.suite.{HttpTestSuite, OrgGenerators}
import com.bbr.commerz.organization.suite.json._
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.{PhoneNumber, StaffAuth, StaffRole}
import io.circe.syntax._
import org.http4s.Method._
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.headers.`Content-Type`
import org.http4s.server.AuthMiddleware

import java.util.UUID

object OrganizationRoutesSpec extends HttpTestSuite with OrgGenerators {

  private val middleware: AuthMiddleware[IO, StaffAuth] = authMiddleware(StaffRole.OWNER)

  def createOrg(newOrganization: Organization): OrganizationService[IO] =
    new OrganizationService[IO](new TestOrganizationRepository {
      override def create(organization: Organization, phoneNumber: PhoneNumber): IO[Organization] =
        IO.pure(
          organization.copy(
            id = newOrganization.id,
            name = newOrganization.name,
            status = newOrganization.status,
            createdAt = newOrganization.createdAt,
            updatedAt = newOrganization.updatedAt
          )
        )
    })

  def failingCreateOrg: OrganizationService[IO] =
    new OrganizationService[IO](new TestOrganizationRepository {
      override def create(organization: Organization, phoneNumber: PhoneNumber): IO[Organization] =
        IO.raiseError(DummyError) *> IO.pure(organization)
    })

  def getAllOrg(organizations: List[Organization]): OrganizationService[IO] =
    new OrganizationService[IO](new TestOrganizationRepository {
      override def getAll(limit: Option[Int], offset: Option[Int]): IO[List[Organization]] =
        IO.pure(organizations)
    })

  def failingGetAllOrg(organizations: List[Organization]): OrganizationService[IO] =
    new OrganizationService[IO](new TestOrganizationRepository {
      override def getAll(limit: Option[Int], offset: Option[Int]): IO[List[Organization]] =
        IO.raiseError(DummyError) *> IO.pure(organizations)
    })

  def getOrgById(organization: Organization): OrganizationService[IO] =
    new OrganizationService[IO](new TestOrganizationRepository {
      override def getById(id: OrgId): IO[Organization] = IO.pure(organization)
    })

  def failingGetOrgById(organization: Organization): OrganizationService[IO] =
    new OrganizationService[IO](new TestOrganizationRepository {
      override def getById(id: OrgId): IO[Organization] = IO.raiseError(DummyError) *> IO.pure(organization)
    })

  def updateOrgById(newOrganization: Organization): OrganizationService[IO] =
    new OrganizationService[IO](new TestOrganizationRepository {
      override def updateById(orgId: OrgId, organization: Organization): IO[Organization] =
        IO.pure(
          organization.copy(
            id = newOrganization.id,
            name = newOrganization.name,
            status = newOrganization.status,
            createdAt = newOrganization.createdAt,
            updatedAt = newOrganization.updatedAt
          )
        )
    })

  def failingUpdateById: OrganizationService[IO] =
    new OrganizationService[IO](new TestOrganizationRepository {
      override def updateById(orgId: OrgId, organization: Organization): IO[Organization] =
        IO.raiseError(DummyError) *> IO.pure(organization)
    })

  def deleteOrgById(): OrganizationService[IO] =
    new OrganizationService[IO](new TestOrganizationRepository {
      override def deleteById(id: OrgId): IO[String] = IO.pure("Organization successfully deleted")
    })

  def failingDeleteOrgById: OrganizationService[IO] =
    new OrganizationService[IO](new TestOrganizationRepository {
      override def deleteById(id: OrgId): IO[String] = IO.raiseError(DummyError) *> IO.pure("Deleted")
    })

  test("POST create organization [CREATED]")(
    forall(genOrganization) { organization =>
      val organizationRequest    = genOrganizationRequest.sample.get
      val req                    = POST(Uri.unsafeFromString("/organization"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(organizationRequest.asJson)
      val routes: HttpRoutes[IO] = new OrganizationRoutes[IO](createOrg(organization)).routes(middleware)
      expectHttpBodyAndStatus(routes, req)(organization, Status.Created)
    }
  )
  test("POST create organization [FAILURE]")(
    forall(genOrganizationRequest) { request =>
      val req                    = POST(Uri.unsafeFromString(s"/organization"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(request.asJson)
      val routes: HttpRoutes[IO] = new OrganizationRoutes[IO](failingCreateOrg).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
  test("POST create organization wrong JSON [FAILURE]")(
    forall(genOrganization) { organization =>
      val req                    = POST(Uri.unsafeFromString("/organization"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(s"$organization")
      val routes: HttpRoutes[IO] = new OrganizationRoutes[IO](failingCreateOrg).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
  test("POST create organization bad request [FAILURE]")(
    forall(genOrganization) { organization =>
      val req                    = POST(Uri.unsafeFromString("/wrong123"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(organization.asJson)
      val routes: HttpRoutes[IO] = new OrganizationRoutes[IO](failingCreateOrg).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )

  test("GET organizations [OK]")(
    forall(genOrganizations) { organizations =>
      val req                    = GET(Uri.unsafeFromString("/organization"))
      val routes: HttpRoutes[IO] =
        new OrganizationRoutes[IO](getAllOrg(organizations)).routes(middleware)
      expectHttpBodyAndStatus(routes, req)(organizations, Status.Ok)
    }
  )
  test("GET organizations [FAILURE]")(
    forall(genOrganizations) { organizations =>
      val req                    = GET(Uri.unsafeFromString("/organization"))
      val routes: HttpRoutes[IO] =
        new OrganizationRoutes[IO](failingGetAllOrg(organizations)).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
  test("GET organizations bad request [FAILURE]")(
    forall(genOrganizations) { organizations =>
      val req                    = GET(Uri.unsafeFromString("/wrong123"))
      val routes: HttpRoutes[IO] =
        new OrganizationRoutes[IO](failingGetAllOrg(organizations)).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )

  test("GET organization by id [OK]")(
    forall(genOrganization) { organization =>
      val id                     = UUID.randomUUID()
      val req                    = GET(Uri.unsafeFromString(s"/organization/$id"))
      val routes: HttpRoutes[IO] =
        new OrganizationRoutes[IO](getOrgById(organization)).routes(middleware)
      expectHttpBodyAndStatus(routes, req)(organization, Status.Ok)
    }
  )
  test("GET organization by id [FAILURE]")(
    forall(genOrganization) { organization =>
      val id                     = UUID.randomUUID()
      val req                    = GET(Uri.unsafeFromString(s"/organization/$id"))
      val routes: HttpRoutes[IO] =
        new OrganizationRoutes[IO](failingGetOrgById(organization)).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
  test("GET organization by id bad request [FAILURE]")(
    forall(genOrganization) { organization =>
      val req                    = GET(Uri.unsafeFromString(s"/inventory/wrong123/${UUID.randomUUID()}"))
      val routes: HttpRoutes[IO] =
        new OrganizationRoutes[IO](failingGetOrgById(organization)).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )

  test("PUT update organization [OK]")(
    forall(genOrganization) { organization =>
      val organizationRequest    = genOrganizationRequest.sample.get
      val req                    = PUT(Uri.unsafeFromString(s"/organization/${UUID.randomUUID()}"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(organizationRequest.asJson)
      val routes: HttpRoutes[IO] =
        new OrganizationRoutes[IO](updateOrgById(organization)).routes(middleware)
      expectHttpBodyAndStatus(routes, req)(organization, Status.Ok)
    }
  )
  test("PUT update organization [FAILURE]")(
    forall(genOrganizationRequest) { request =>
      val req                    = PUT(Uri.unsafeFromString(s"/organization/${UUID.randomUUID()}"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(request.asJson)
      val routes: HttpRoutes[IO] = new OrganizationRoutes[IO](failingUpdateById).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
  test("PUT update organization wrong JSON [FAILURE]")(
    forall(genOrgId) { orgId =>
      val req                    = PUT(Uri.unsafeFromString(s"/organization/${orgId.value}"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(s"$orgId")
      val routes: HttpRoutes[IO] = new OrganizationRoutes[IO](failingUpdateById).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
  test("PUT update organization bad request [FAILURE]")(
    forall(genOrganization) { organization =>
      val req                    = PUT(Uri.unsafeFromString(s"/wrong123"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(organization.asJson)
      val routes: HttpRoutes[IO] = new OrganizationRoutes[IO](failingUpdateById).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )

  test("DELETE organization [OK]")(
    forall(genOrgId) { orgId =>
      val req                    = DELETE(Uri.unsafeFromString(s"/organization/${orgId.value}"))
      val routes: HttpRoutes[IO] = new OrganizationRoutes[IO](deleteOrgById()).routes(middleware)
      expectHttpStatus(routes, req)(Status.Ok)
    }
  )
  test("DELETE organization [FAILURE]")(
    forall(genOrgId) { orgId =>
      val req                    = DELETE(Uri.unsafeFromString(s"/organization/${orgId.value}"))
      val routes: HttpRoutes[IO] = new OrganizationRoutes[IO](failingDeleteOrgById).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
  test("DELETE organization bad request [FAILURE]")(
    forall(genOrgId) { orgId =>
      val req                    = DELETE(Uri.unsafeFromString(s"/inventory/wrong123/${orgId.value}"))
      val routes: HttpRoutes[IO] = new OrganizationRoutes[IO](failingDeleteOrgById).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
}

protected class TestOrganizationRepository extends OrganizationAlgebra[IO] {
  override def create(organization: Organization, phoneNumber: PhoneNumber): IO[Organization] = organization.pure[IO]

  override def getAll(limit: Option[Int], offset: Option[Int]): IO[List[Organization]] = IO.pure(List.empty)

  override def getById(id: OrgId): IO[Organization] = null

  override def updateById(orgId: OrgId, organization: Organization): IO[Organization] = organization.pure[IO]

  override def deleteById(id: OrgId): IO[String] = "Successfully deleted".pure[IO]

  override def getAll(orgName: OrganizationName, limit: Int, offset: Int): IO[List[Organization]] = IO.pure(List.empty)

  override def checkOrganizationExistence(orgId: OrgId): IO[Boolean] = true.pure[IO]
}
