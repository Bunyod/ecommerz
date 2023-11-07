package com.bbr.commerz.auth.http.auth

import cats.effect.Async
import com.bbr.commerz.auth.domain.auth.AuthPayloads.PasswordUpdate
import com.bbr.commerz.auth.domain.auth.AuthService
import com.bbr.commerz.auth.http.utils.json.passwordUpdateDecoder
import com.bbr.platform.domain.Staff.StaffAuth
import com.bbr.platform.utils.decoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s._
import org.http4s.server._

class AuthRoutes[F[_]: Async](
  auth: AuthService[F]
) extends Http4sDsl[F] {

  private[auth] val pathPrefix = "/org"

  private val httpRoutes: AuthedRoutes[StaffAuth, F] = AuthedRoutes.of {
    case ar @ POST -> Root / UUIDVar(_) / "password" as staff =>
      ar.req.decodeR[PasswordUpdate] { request =>
        Ok(auth.updatePassword(staff.id, staff.role, request))
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, StaffAuth]): HttpRoutes[F] = Router(
    pathPrefix -> authMiddleware(httpRoutes)
  )

}
