package com.bbr.commerz.organization.http.staff

import cats.implicits._
import cats.effect.Async
import com.bbr.commerz.organization.domain.staff.StaffPayloads.StaffUpdate
import com.bbr.commerz.organization.domain.staff.StaffService
import com.bbr.commerz.organization.http.utils.json._
import com.bbr.platform.UuidOpts
import com.bbr.platform.utils.decoder._
import com.bbr.platform.domain.Staff.StaffAuth
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final class StaffRoutes[F[_]: Async](
  staffService: StaffService[F]
) extends Http4sDsl[F] {

  private val httpRoutes: AuthedRoutes[StaffAuth, F] = AuthedRoutes.of {

    case ar @ PUT -> Root / UUIDVar(orgId) / "staff" as StaffAuth(staffId, _, _, _) =>
      ar.req.decodeR[StaffUpdate] { request =>
        Ok(staffService.updateById(orgId.toOrgId, staffId, request))
          .recoverWith(error => InternalServerError(error.getMessage))
      }
  }

  private[http] val pathPrefix = "/org"

  def routes(authMiddleware: AuthMiddleware[F, StaffAuth]): HttpRoutes[F] = Router(
    pathPrefix -> authMiddleware(httpRoutes)
  )
}
