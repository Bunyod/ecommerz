package com.bbr.commerz.organization.http.staff

import cats.effect._
import cats.effect.unsafe.implicits.global
import cats.implicits._
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff._
import com.bbr.commerz.organization.domain.staff.StaffPayloads._
import com.bbr.commerz.organization.domain.staff.{StaffAlgebra, StaffService}
import com.bbr.commerz.organization.http.authMiddleware
import com.bbr.commerz.organization.suite.json._
import com.bbr.commerz.organization.suite.{HttpTestSuite, OrgGenerators}
import com.bbr.platform.config.Configuration.PasswordSalt
import com.bbr.platform.crypto.{CryptoAlgebra, CryptoService}
import eu.timepit.refined.api.Refined
import io.circe.syntax._
import org.http4s.Method._
import org.http4s._
import org.http4s.circe.jsonEncoder
import org.http4s.client.dsl.io._
import org.http4s.headers.`Content-Type`
import org.http4s.server.AuthMiddleware

import java.util.UUID

object OwnerRoutesSpec extends HttpTestSuite with OrgGenerators {

  private val middleware: AuthMiddleware[IO, StaffAuth] = authMiddleware(StaffRole.OWNER)

  private val salt: PasswordSalt = PasswordSalt(Refined.unsafeApply("$2a$10$8DU8P4l/N2e9EQedx9APC."))

  val crypto: CryptoAlgebra = CryptoService.make[IO](salt).unsafeRunSync()
  val staffObject: Staff    = genAgent.sample.get

  def buildStaffRequest(staff: Staff): StaffRequest =
    StaffRequest(
      branchId = staff.branchId,
      role = staff.role,
      userName = UserNameParam(Refined.unsafeApply(staff.userName.value)),
      password = PasswordParam(Refined.unsafeApply(staff.password.value)),
      email = staff.email.map(s => EmailParam(Refined.unsafeApply(s.value))),
      phoneNumber = PhoneNumberParam(Refined.unsafeApply(staff.phoneNumber.value)),
      firstName = staff.firstName.map(s => FirstNameParam(Refined.unsafeApply(s.value))),
      lastName = staff.lastName.map(s => LastNameParam(Refined.unsafeApply(s.value))),
      birthDate = staff.birthDate
    )

  def deleteById(): StaffService[IO] =
    new StaffService[IO](
      new TestStaffRepository {
        override def deleteById(orgId: OrgId, staffId: StaffId): IO[String] = IO.pure("Successfully deleted")
      },
      crypto
    )

  def failingDeleteById(): StaffService[IO] =
    new StaffService[IO](
      new TestStaffRepository {
        override def deleteById(orgId: OrgId, staffId: StaffId): IO[String] =
          IO.raiseError(DummyError) *> IO.pure("Successfully deleted")
      },
      crypto
    )

  def getWorkers(staff: List[Staff]): StaffService[IO] =
    new StaffService[IO](
      new TestStaffRepository {
        override def getWorkers(orgId: OrgId, username: Option[String], limit: Int, offset: Int): IO[List[Staff]] =
          IO.pure(staff)
      },
      crypto
    )

  def failingGetWorkers(): StaffService[IO] =
    new StaffService[IO](
      new TestStaffRepository {
        override def getWorkers(orgId: OrgId, username: Option[String], limit: Int, offset: Int): IO[List[Staff]] =
          IO.raiseError(DummyError) *> List.empty[Staff].pure[IO]
      },
      crypto
    )

  def getById(staff: Staff): StaffService[IO] = new StaffService[IO](
    new TestStaffRepository { override def getById(orgId: OrgId, staffId: StaffId): IO[Staff] = IO.pure(staff) },
    crypto
  )

  def failingGetById(): StaffService[IO] = new StaffService[IO](
    new TestStaffRepository {
      override def getById(orgId: OrgId, staffId: StaffId): IO[Staff] = IO.raiseError(DummyError)
    },
    crypto
  )

  def updateById(newStaff: Staff): StaffService[IO] =
    new StaffService[IO](
      new TestStaffRepository {
        override def updateById(branchId: StaffId, staff: Staff): IO[Staff] =
          newStaff.pure[IO]
      },
      crypto
    )

  def failingUpdateById(): StaffService[IO] =
    new StaffService[IO](
      new TestStaffRepository {
        override def updateById(staffId: StaffId, staff: Staff): IO[Staff] =
          IO.raiseError(DummyError)
      },
      crypto
    )

  def newStaff(st: Staff): StaffService[IO] =
    new StaffService[IO](
      new TestStaffRepository { override def create(staff: Staff): IO[Staff] = st.pure[IO] },
      crypto
    )

  def failingNewStaff(st: Staff): StaffService[IO] = new StaffService[IO](
    new TestStaffRepository {
      override def create(staff: Staff): IO[Staff] =
        IO.raiseError(DummyError) *> st.pure[IO]
    },
    crypto
  )

  test("POST create staff [CREATED]")(
    forall(genWorker) { staff =>
      val staffRequest           = buildStaffRequest(staff)
      val req                    = POST(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/staff"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(staffRequest.asJson)
      val routes: HttpRoutes[IO] = new OwnerRoutes[IO](newStaff(staff)).routes(middleware)
      expectHttpBodyAndStatus(routes, req)(staff, Status.Ok)
    }
  )
  test("POST create staff [FAILURE]")(
    forall(genWorker) { staff =>
      val staffRequest           = buildStaffRequest(staff)
      val req                    = POST(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/staff"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(staffRequest.asJson)
      val routes: HttpRoutes[IO] = new OwnerRoutes[IO](failingNewStaff(staff)).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
  test("POST create staff wrong JSON [FAILURE]")(
    forall(genWorker) { staff =>
      val staffRequest           = buildStaffRequest(staff)
      val req                    = POST(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/staff"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(s"Wrong: ${staffRequest.asJson}")
      val routes: HttpRoutes[IO] = new OwnerRoutes[IO](failingNewStaff(staff)).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
  test("POST create staff bad request [FAILURE]")(
    forall(genWorker) { staff =>
      val staffRequest           = buildStaffRequest(staff)
      val req                    = POST(Uri.unsafeFromString(s"/wrong123/staff/registration"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(s"Wrong: ${staffRequest.asJson}")
      val routes: HttpRoutes[IO] = new OwnerRoutes[IO](failingNewStaff(staff)).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )

  test("GET staff [OK]")(
    forall(genAgents) { staff =>
      val req                    = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/staff"))
      val routes: HttpRoutes[IO] = new OwnerRoutes[IO](getWorkers(staff)).routes(middleware)
      expectHttpBodyAndStatus(routes, req)(staff, Status.Ok)
    }
  )
  test("GET staff [FAILURE]")(
    forall(genAgents) { _ =>
      val req                    = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/staff"))
      val routes: HttpRoutes[IO] = new OwnerRoutes[IO](failingGetWorkers()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
  test("GET staff bad request [FAILURE]")(
    forall(genAgents) { _ =>
      val req                    = GET(Uri.unsafeFromString(s"wrong123/org/${UUID.randomUUID()}/staff/${UUID.randomUUID()}"))
      val routes: HttpRoutes[IO] = new OwnerRoutes[IO](failingGetWorkers()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )

  test("GET staff by id [OK]")(
    forall(genWorker) { staff =>
      val req                    = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/staff/${UUID.randomUUID()}"))
      val routes: HttpRoutes[IO] = new OwnerRoutes[IO](getById(staff)).routes(middleware)
      expectHttpBodyAndStatus(routes, req)(staff, Status.Ok)
    }
  )
  test("GET staff by id [FAILURE]")(
    forall(genWorker) { _ =>
      val req                    = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/staff/${UUID.randomUUID()}"))
      val routes: HttpRoutes[IO] = new OwnerRoutes[IO](failingGetById()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
  test("GET staff by id bad request [FAILURE]")(
    forall(genWorker) { _ =>
      val req                    = GET(Uri.unsafeFromString(s"/wrong/org/${UUID.randomUUID()}/staff/${UUID.randomUUID()}"))
      val routes: HttpRoutes[IO] = new OwnerRoutes[IO](failingGetById()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )

  test("PUT update staff [OK]")(
    forall(genWorker) { staff =>
      val staffRequest           = buildStaffRequest(staff)
      val req                    = PUT(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/staff/${UUID.randomUUID()}"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(staffRequest.asJson)
      val routes: HttpRoutes[IO] = new OwnerRoutes[IO](updateById(staff)).routes(middleware)
      expectHttpBodyAndStatus(routes, req)(staff, Status.Ok)
    }
  )
  test("PUT update staff [FAILURE]")(
    forall(genWorker) { staff =>
      val staffRequest           = buildStaffRequest(staff)
      val req                    = PUT(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/staff/${UUID.randomUUID()}"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(staffRequest.asJson)
      val routes: HttpRoutes[IO] = new OwnerRoutes[IO](failingUpdateById()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
  test("PUT update staff wrong JSON [FAILURE]")(
    forall(genWorker) { staff =>
      val staffRequest           = buildStaffRequest(staff)
      val req                    = PUT(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/staff/${UUID.randomUUID()}"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(s"Wrong: $staffRequest")
      val routes: HttpRoutes[IO] = new OwnerRoutes[IO](failingUpdateById()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )

  test("DELETE staff [OK]")(
    forall(UUID.randomUUID()) { id =>
      val req                    = DELETE(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/staff/$id"))
      val routes: HttpRoutes[IO] = new OwnerRoutes[IO](deleteById()).routes(middleware)
      expectHttpStatus(routes, req)(Status.Ok)
    }
  )
  test("DELETE staff [FAILURE]")(
    forall(UUID.randomUUID()) { id =>
      val req                    = DELETE(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/staff/$id"))
      val routes: HttpRoutes[IO] = new OwnerRoutes[IO](failingDeleteById()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )
  test("DELETE staff bad request [FAILURE]")(
    forall(UUID.randomUUID()) { id =>
      val req                    = DELETE(Uri.unsafeFromString(s"/wrong123/$id"))
      val routes: HttpRoutes[IO] = new OwnerRoutes[IO](failingDeleteById()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )

}

protected[http] class TestStaffRepository extends StaffAlgebra[IO] {

  import OwnerRoutesSpec.staffObject

  override def checkPhoneNumber(orgId: OrgId, phoneNumber: PhoneNumber): IO[Boolean] = true.pure[IO]

  override def create(staff: Staff): IO[Staff] = staff.pure[IO]

  override def getWorkers(orgId: OrgId, username: Option[String] = None, limit: Int, offset: Int): IO[List[Staff]] =
    List.empty[Staff].pure[IO]

  override def getAgents(orgId: OrgId, username: Option[String], limit: Int, offset: Int): IO[List[Staff]] =
    List.empty[Staff].pure[IO]

  override def getAll(orgId: OrgId, username: Option[String], limit: Int, offset: Int): IO[List[Staff]] =
    List.empty[Staff].pure[IO]

  override def getById(orgId: OrgId, staffId: StaffId): IO[Staff] = staffObject.pure[IO]

  override def updateById(staffId: StaffId, staff: Staff): IO[Staff] = staffObject.pure[IO]

  override def deleteById(orgId: OrgId, staffId: StaffId): IO[String] = "Deleted successfully".pure[IO]
}
