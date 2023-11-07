package com.bbr.commerz.organization.suite

import scala.util.control.NoStackTrace
import cats.effect._
import cats.implicits._
import io.circe._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import weaver.scalacheck._
import weaver._

trait HttpTestSuite extends SimpleIOSuite with Checkers {

  case object DummyError extends NoStackTrace

  def expectHttpBodyAndStatus[A: Encoder](routes: HttpRoutes[IO], req: Request[IO])(
    expectedBody: A,
    expectedStatus: Status
  ): IO[Expectations] =
    routes.run(req).value.flatMap {
      case Some(resp) =>
        resp.asJson.map { json =>
          // Expectations form a multiplicative Monoid but we can also use other combinators like `expect.all`
          expect.same(resp.status, expectedStatus) |+| expect
            .same(json.dropNullValues, expectedBody.asJson.dropNullValues)
        }
      case None => IO.pure(failure("route not found"))
    }

  def expectHttpStatus(routes: HttpRoutes[IO], req: Request[IO])(expectedStatus: Status): IO[Expectations] =
    routes.run(req).value.map {
      case Some(resp) => expect.same(resp.status, expectedStatus)
      case None => failure("route not found")
    }

  def expectHttpFailure(routes: HttpRoutes[IO], req: Request[IO]): IO[Expectations] =
    routes
      .run(req)
      .value
      .flatMap(r => r.map(IO.pure).getOrElse(IO.raiseError(new RuntimeException("Unexpected error happened"))))
      .attempt
      .map {
        case Left(_) => success
        case Right(er) if er.status.code == 422 => success
        case Right(_) => failure(s"expected a failure")
      }

}
