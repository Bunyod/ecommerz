package com.bbr.commerz.organization.http.staff

import cats.implicits._
import cats.effect.Async
import com.bbr.commerz.organization.domain.staff.StaffPayloads._
import com.bbr.commerz.organization.domain.staff.StaffService
import com.bbr.commerz.organization.http.utils.json._
import com.bbr.platform.UuidOpts
import com.bbr.platform.utils.decoder._
import com.bbr.platform.domain.Staff.StaffRole.OWNER
import com.bbr.platform.domain.Staff.StaffAuth
import com.bbr.platform.http.QueryParameters
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.server.{AuthMiddleware, Router}

final class OwnerRoutes[F[_]: Async](
  staffService: StaffService[F]
) extends QueryParameters[F] {

  private val httpRoutes: AuthedRoutes[StaffAuth, F] = AuthedRoutes.of {

    case ar @ POST -> Root / UUIDVar(orgId) / "staff" as StaffAuth(_, _, _, OWNER) =>
      ar.req.decodeR[StaffRequest] { staff =>
        Ok(staffService.create(orgId.toOrgId, staff))
          .recoverWith {
            case PhoneNumberInUse(pn) => BadRequest(s"This phone number already exists: $pn")
            case error                => InternalServerError(error.getMessage)
          }
      }

    case GET -> Root / UUIDVar(orgId) / "staff" / UUIDVar(staffId) as StaffAuth(_, _, _, OWNER) =>
      Ok(staffService.getById(orgId.toOrgId, staffId.toStaffId))
        .recoverWith(error => InternalServerError(error.getMessage))

    case GET -> Root / UUIDVar(orgId) / "staff" :? UserNameQueryParamMatcher(
          username
        ) +& LimitQueryParamMatcher(
          limit
        ) +& OffsetQueryParamMatcher(offset) as _ =>
      Ok(staffService.getWorkers(orgId.toOrgId, username, limit, offset))
        .recoverWith(error => InternalServerError(error.getMessage))

    case ar @ PUT -> Root / UUIDVar(orgId) / "staff" / UUIDVar(staffId) as StaffAuth(_, _, _, OWNER) =>
      ar.req.decodeR[StaffUpdate] { request =>
        Ok(staffService.updateById(orgId.toOrgId, staffId.toStaffId, request))
          .recoverWith(error => InternalServerError(error.getMessage))
      }

    case _ @DELETE -> Root / UUIDVar(orgId) / "staff" / UUIDVar(staffId) as StaffAuth(_, _, _, OWNER) =>
      Ok(staffService.deleteById(orgId.toOrgId, staffId.toStaffId))
        .recoverWith(error => InternalServerError(error.getMessage))
  }

  private[http] val pathPrefix = "/org"

  def routes(authMiddleware: AuthMiddleware[F, StaffAuth]): HttpRoutes[F] = Router(
    pathPrefix -> authMiddleware(httpRoutes)
  )

}
