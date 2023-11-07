package com.bbr.commerz.auth.http.auth

import cats.effect.Async
import cats.implicits._
import com.bbr.commerz.auth.domain.auth.AuthService
import com.bbr.platform.domain.Staff.{PhoneNumber, StaffAuth}
import dev.profunktor.auth.AuthHeaders
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s._
import org.http4s.server._

class LogoutRoutes[F[_]: Async](
  auth: AuthService[F]
) extends Http4sDsl[F] {

  private[auth] val pathPrefix = "/org"

  private val httpRoutes: AuthedRoutes[StaffAuth, F] = AuthedRoutes.of {
    case ar @ POST -> Root / UUIDVar(_) / "staff" / "logout" as staff =>
      AuthHeaders
        .getBearerToken[F](ar.req)
        .traverse_(t => auth.logout(t, PhoneNumber(staff.phoneNumber.value))) *> NoContent()

  }

  def routes(authMiddleware: AuthMiddleware[F, StaffAuth]): HttpRoutes[F] = Router(
    pathPrefix -> authMiddleware(httpRoutes)
  )
}
