package com.bbr.commerz.organization.http.staff

import cats.effect._
import cats.implicits._
import com.bbr.platform.domain.Staff._
import OwnerRoutesSpec.{staffObject, crypto, buildStaffRequest}
import com.bbr.commerz.organization.domain.staff.StaffPayloads._
import com.bbr.commerz.organization.domain.staff.StaffService
import com.bbr.commerz.organization.http.authMiddleware
import com.bbr.commerz.organization.http.utils.json._
import com.bbr.commerz.organization.suite.{HttpTestSuite, OrgGenerators}
import io.circe.syntax._
import org.http4s.Method._
import org.http4s._
import org.http4s.circe.jsonEncoder
import org.http4s.client.dsl.io._
import org.http4s.headers.`Content-Type`
import org.http4s.server.AuthMiddleware

import java.util.UUID
import scala.util.Random

object StaffRoutesSpec extends HttpTestSuite with OrgGenerators {

  val role: StaffRole = Random.shuffle(List(StaffRole.WORKER, StaffRole.AGENT)).head

  private val middleware: AuthMiddleware[IO, StaffAuth] = authMiddleware(role)

  def updateById(newStaff: Staff): StaffService[IO] =
    new StaffService[IO](
      new TestStaffRepository {
        override def updateById(branchId: StaffId, staff: Staff): IO[Staff] = newStaff.pure[IO]
      },
      crypto
    )

  def failingUpdateById(): StaffService[IO] =
    new StaffService[IO](
      new TestStaffRepository {
        override def updateById(staffId: StaffId, staff: Staff): IO[Staff] =
          IO.raiseError(DummyError) *> IO.pure(staffObject)
      },
      crypto
    )

  test("PUT update staff [OK]")(
    forall(genWorker) { staff =>
      val staffRequest           = buildStaffRequest(staff)
      val req                    = PUT(Uri.unsafeFromString(s"/org/${UUID.randomUUID}/staff"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(staffRequest.asJson)
      val routes: HttpRoutes[IO] = new StaffRoutes[IO](updateById(staff)).routes(middleware)
      expectHttpBodyAndStatus(routes, req)(staff, Status.Ok)
    }
  )

  test("PUT update staff [FAILURE]")(
    forall(genWorker) { staff =>
      val staffRequest           = buildStaffRequest(staff)
      val req                    = PUT(Uri.unsafeFromString(s"/org/${UUID.randomUUID}/staff"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(staffRequest.asJson)
      val routes: HttpRoutes[IO] = new StaffRoutes[IO](failingUpdateById()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )

  test("PUT update staff wrong JSON [FAILURE]")(
    forall(genWorker) { staff =>
      val staffRequest           = buildStaffRequest(staff)
      val req                    = PUT(Uri.unsafeFromString(s"/org/${UUID.randomUUID}/staff"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(s"Wrong: $staffRequest")
      val routes: HttpRoutes[IO] = new StaffRoutes[IO](failingUpdateById()).routes(middleware)
      expectHttpFailure(routes, req)
    }
  )

}
