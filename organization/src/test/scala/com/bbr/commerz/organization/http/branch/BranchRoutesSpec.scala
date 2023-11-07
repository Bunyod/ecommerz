package com.bbr.commerz.organization.http.branch

import cats.effect._
import cats.implicits._
import com.bbr.commerz.organization.domain.branch.BranchPayloads._
import com.bbr.commerz.organization.domain.branch._
import com.bbr.commerz.organization.http.authMiddleware
import com.bbr.commerz.organization.suite.{HttpTestSuite, OrgGenerators}
import com.bbr.commerz.organization.suite.json._
import com.bbr.platform.domain.Branch.BranchId
import org.http4s.server.AuthMiddleware
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff._
import io.circe.syntax._
import org.http4s.Method._
import org.http4s._
import org.http4s.circe.jsonEncoder
import org.http4s.client.dsl.io._
import org.http4s.headers.`Content-Type`

import java.util.UUID

object BranchRoutesSpec extends HttpTestSuite with OrgGenerators {

  private val middleware: AuthMiddleware[IO, StaffAuth] = authMiddleware(StaffRole.OWNER)

  private def createBranch(newBranch: Branch): BranchService[IO] =
    new BranchService[IO](new TestBranchRepository {
      override def create(branch: Branch): IO[Branch] = newBranch.pure[IO]
    })

  private def failingCreateBranch(): BranchService[IO] =
    new BranchService[IO](new TestBranchRepository {
      override def create(branch: Branch): IO[Branch] = IO.raiseError(DummyError) *> IO.pure(branch)
    })

  private def deleteById(): BranchService[IO] =
    new BranchService[IO](new TestBranchRepository {
      override def deleteById(orgId: OrgId, branchId: BranchId): IO[String] = IO.pure("Successfully deleted")
    })

  private def failingDeleteById(): BranchService[IO] =
    new BranchService[IO](new TestBranchRepository {
      override def deleteById(orgId: OrgId, branchId: BranchId): IO[String] =
        IO.raiseError(DummyError) *> IO.pure("Successfully deleted")
    })

  private def getById(branch: Branch): BranchService[IO] =
    new BranchService[IO](new TestBranchRepository {
      override def getById(orgId: OrgId, branchId: BranchId): IO[Branch] = IO.pure(branch)
    })

  private def failingGetById(): BranchService[IO] =
    new BranchService[IO](new TestBranchRepository {
      override def getById(orgId: OrgId, branchId: BranchId): IO[Branch] =
        IO.raiseError(DummyError) *> IO.pure(branchObject)
    })

  private def updateById(newBranch: Branch): BranchService[IO] =
    new BranchService[IO](new TestBranchRepository {
      override def updateById(branchId: BranchId, branch: Branch): IO[Branch] = newBranch.pure[IO]
    })

  private def failingUpdateById(): BranchService[IO] =
    new BranchService[IO](new TestBranchRepository {
      override def updateById(branchId: BranchId, branch: Branch): IO[Branch] =
        IO.raiseError(DummyError) *> IO.pure(branchObject)
    })

  private def getBranchesByParam(branches: List[Branch]): BranchService[IO] =
    new BranchService[IO](new TestBranchRepository {
      override def getAll(orgId: OrgId, branchName: Option[BranchName]): IO[List[Branch]] =
        IO.pure(branches)
    })

  private def failingGetBranchesByParam(): BranchService[IO] =
    new BranchService[IO](new TestBranchRepository {
      override def getAll(orgId: OrgId, branchName: Option[BranchName]): IO[List[Branch]] =
        IO.raiseError(DummyError)
    })

  test("POST create branch [CREATED]") {
    forall(genBranch) { branch =>
      val branchRequest          = genBranchRequest.sample.get
      val req                    = POST(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/branches"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(branchRequest.asJson)
      val routes: HttpRoutes[IO] = new BranchRoutes[IO](createBranch(branch)).routes(middleware)
      expectHttpBodyAndStatus(routes, req)(branch, Status.Created)
    }
  }
  test("POST create branch [FAILURE]")(
    forall(genBranchRequest) { request =>
      val req                    = POST(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/branches"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(request.asJson)
      val routes: HttpRoutes[IO] = new BranchRoutes[IO](failingCreateBranch()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
  test("POST create branch wrong JSON [FAILURE]")(
    forall(genBranchRequest) { request =>
      val req                    = POST(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/branches"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(s"Wrong: $request")
      val routes: HttpRoutes[IO] = new BranchRoutes[IO](failingCreateBranch()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
  test("POST create branch bad request [FAILURE]")(
    forall(genBranchRequest) { request =>
      val req                    = POST(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/branches"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(request.asJson)
      val routes: HttpRoutes[IO] = new BranchRoutes[IO](failingCreateBranch()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )

  test("GET branches [OK]")(
    forall(genBranches) { branches =>
      val req                    = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/branches"))
      val routes: HttpRoutes[IO] = new BranchRoutes[IO](getBranchesByParam(branches)).routes(middleware)
      expectHttpBodyAndStatus(routes, req)(branches, Status.Ok)
    }
  )
  test("GET branches [FAILURE]")(
    forall(genOrgId) { orgId =>
      val req                    = GET(Uri.unsafeFromString(s"/org/${orgId.value}/branches"))
      val routes: HttpRoutes[IO] =
        new BranchRoutes[IO](failingGetBranchesByParam()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
  test("GET branches bad request [FAILURE]")(
    forall(genOrgId) { orgId =>
      val req                    = GET(Uri.unsafeFromString(s"/org/${orgId.value}/branches"))
      val routes: HttpRoutes[IO] =
        new BranchRoutes[IO](failingGetBranchesByParam()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )

  test("GET branch by id [OK]")(
    forall(genBranch) { branch =>
      val id                     = UUID.randomUUID()
      val req                    = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/branches/$id"))
      val routes: HttpRoutes[IO] = new BranchRoutes[IO](getById(branch)).routes(middleware)
      expectHttpBodyAndStatus(routes, req)(branch, Status.Ok)
    }
  )
  test("GET branch by id [FAILURE]")(
    forall(genBranch) { _ =>
      val id                     = UUID.randomUUID()
      val req                    = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/branches/$id"))
      val routes: HttpRoutes[IO] =
        new BranchRoutes[IO](failingGetById()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
  test("GET branch by id bad request [FAILURE]")(
    forall(genBranch) { _ =>
      val id                     = UUID.randomUUID()
      val req                    = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/branches/$id"))
      val routes: HttpRoutes[IO] =
        new BranchRoutes[IO](failingGetById()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )

  test("PUT update branch [OK]")(
    forall(genBranch) { branch =>
      val branchUpdate           = genBranchUpdate.sample.get
      val req                    = PUT(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/branches/${UUID.randomUUID()}"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(branchUpdate.asJson)
      val routes: HttpRoutes[IO] =
        new BranchRoutes[IO](updateById(branch)).routes(middleware)
      expectHttpBodyAndStatus(routes, req)(branch, Status.Ok)
    }
  )
  test("PUT update branch [FAILURE]")(
    forall(genBranchUpdate) { branchUpdate =>
      val req                    = PUT(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/branches/${UUID.randomUUID()}"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(branchUpdate.asJson)
      val routes: HttpRoutes[IO] = new BranchRoutes[IO](failingUpdateById()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
  test("PUT update branch bad request [FAILURE]")(
    forall(genBranch) { branch =>
      val req                    = PUT(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/branches/${UUID.randomUUID()}"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(branch.asJson)
      val routes: HttpRoutes[IO] = new BranchRoutes[IO](failingUpdateById()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
  test("PUT create branch wrong JSON [FAILURE]")(
    forall(genOrgId) { orgId =>
      val branchUpdate           = genBranchUpdate.sample.get
      val req                    = PUT(Uri.unsafeFromString(s"/org/${orgId.value}/branches/${UUID.randomUUID()}"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(s"Wrong: $branchUpdate")
      val routes: HttpRoutes[IO] = new BranchRoutes[IO](failingUpdateById()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )

  test("DELETE branch [OK]")(
    forall(genOrgId) { orgId =>
      val req                    = DELETE(Uri.unsafeFromString(s"/org/${orgId.value}/branches/${UUID.randomUUID()}"))
      val routes: HttpRoutes[IO] = new BranchRoutes[IO](deleteById()).routes(middleware)
      expectHttpStatus(routes, req)(Status.Ok)
    }
  )
  test("DELETE branch [FAILURE]")(
    forall(genOrgId) { orgId =>
      val req                    = DELETE(Uri.unsafeFromString(s"/org/${orgId.value}/branches/${UUID.randomUUID()}"))
      val routes: HttpRoutes[IO] = new BranchRoutes[IO](failingDeleteById()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
  test("DELETE branch bad request [FAILURE]")(
    forall(genOrgId) { id =>
      val req                    = DELETE(Uri.unsafeFromString(s"/org/${id.value}/branches/${UUID.randomUUID()}"))
      val routes: HttpRoutes[IO] = new BranchRoutes[IO](failingDeleteById()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )

}

protected class TestBranchRepository extends BranchAlgebra[IO] with OrgGenerators {

  val branchObject: Branch = genBranch.sample.get

  override def getByName(branchName: BranchName, orgId: OrgId): IO[Option[Branch]] = IO.pure(None)

  override def activateBranch(orgId: OrgId, branchName: BranchName): IO[Unit] = IO.pure(())

  override def create(branch: Branch): IO[Branch] = branch.pure[IO]

  override def getAll(
    orgId: OrgId,
    branchName: Option[BranchName]
  ): IO[List[Branch]] = List.empty[Branch].pure[IO]

  override def getById(orgId: OrgId, id: BranchId): IO[Branch] = branchObject.pure[IO]

  override def updateById(branchId: BranchId, branch: Branch): IO[Branch] = branchObject.pure[IO]

  override def deleteById(orgId: OrgId, id: BranchId): IO[String] = "Deleted successfully".pure[IO]

  override def checkBranchExistence(branchId: BranchId): IO[Boolean] = true.pure[IO]
}
