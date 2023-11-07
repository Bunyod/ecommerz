package com.bbr.commerz.auth.http.auth

import cats.effect.Async
import cats.implicits._
import com.bbr.commerz.auth.domain.auth.AuthPayloads._
import com.bbr.commerz.auth.domain.auth.AuthService
import com.bbr.commerz.auth.http.utils.json._
import com.bbr.platform.utils.decoder._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

final class LoginRoutes[F[_]: Async](
  auth: AuthService[F]
) extends Http4sDsl[F] {

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" =>
      req.decodeR[LoginStaff] { user =>
        auth
          .login(user.phoneNumber, user.password.toDomain)
          .flatMap(Ok(_))
          .recoverWith {
            case InvalidUserOrPassword(pn) =>
              BadRequest(s"Invalid phone number, password or organization: phone number: $pn;")
            case error                     => InternalServerError(error.getMessage)
          }
      }

    case req @ POST -> Root / "recovery" =>
      req.decodeR[RecoveryRequest] { request =>
        auth
          .recover(request.phoneNumber.toDomain, request.organizationName.toDomain)
          .flatMap(_ => Ok("Recovery code has been sent."))
          .recoverWith(error => InternalServerError(error.getMessage))
      }

    case req @ POST -> Root / "recovery" / "verify" =>
      req.decodeR[RecoveryDataRequest] { request =>
        auth
          .verify(request.toDomain)
          .flatMap(_ => Ok("The code successfully verified."))
          .recoverWith(error => InternalServerError(error.getMessage))
      }

    case req @ POST -> Root / "recovery" / "password" =>
      req.decodeR[PasswordRecovery] { request =>
        auth
          .updatePassword(request.phoneNumber.toDomain, request.password.toDomain, request.orgName.toDomain)
          .flatMap(_ => Ok("The password successfully updated."))
          .recoverWith(error => InternalServerError(error.getMessage))
      }

  }

}
