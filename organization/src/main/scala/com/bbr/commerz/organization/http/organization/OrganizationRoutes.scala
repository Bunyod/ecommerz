package com.bbr.commerz.organization.http.organization

import cats.implicits._
import cats.effect.Async
import com.bbr.commerz.organization.domain.organization.OrganizationService
import com.bbr.platform.UuidOpts
import com.bbr.platform.http.QueryParameters
import com.bbr.platform.utils.decoder.RefinedRequestDecoder
import com.bbr.commerz.organization.domain.organization.OrganizationPayloads.{OrganizationName, OrganizationRequest}
import com.bbr.commerz.organization.http.utils.json._
import com.bbr.platform.domain.Staff.StaffAuth
import com.bbr.platform.domain.Staff.StaffRole.OWNER
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}

class OrganizationRoutes[F[_]: Async](
  organizationService: OrganizationService[F]
) extends QueryParameters[F] {

  private val httpRoutes: AuthedRoutes[StaffAuth, F] = AuthedRoutes.of {

    case ar @ POST -> Root as StaffAuth(_, _, phoneNumber, OWNER) =>
      ar.req.decodeR[OrganizationRequest] { organization =>
        Created(organizationService.create(organization, phoneNumber))
          .recoverWith(error => InternalServerError(error.getMessage))
      }

    case GET -> Root / UUIDVar(orgId) as StaffAuth(_, _, _, OWNER) =>
      Ok(organizationService.getById(orgId.toOrgId))
        .recoverWith(error => InternalServerError(error.getMessage))

    case GET -> Root :? NameQueryParamMatcher(
          name
        ) +& LimitQueryParamMatcher(
          limit
        ) +& OffsetQueryParamMatcher(offset) as StaffAuth(_, _, _, OWNER) =>
      (name, limit, offset) match {
        case (Some(name), limit, offset) =>
          Ok(organizationService.getAll(OrganizationName(name), limit, offset))
            .recoverWith(error => InternalServerError(error.getMessage))
        case (None, limit, offset)       =>
          Ok(organizationService.getAll(limit, offset))
            .recoverWith(error => InternalServerError(error.getMessage))
      }

    case ar @ PUT -> Root / UUIDVar(orgId) as StaffAuth(_, _, _, OWNER) =>
      ar.req.decodeR[OrganizationRequest] { organization =>
        Ok(organizationService.updateById(orgId.toOrgId, organization))
          .recoverWith(error => InternalServerError(error.getMessage))
      }

    case DELETE -> Root / UUIDVar(orgId) as StaffAuth(_, _, _, OWNER) =>
      Ok(organizationService.deleteById(orgId.toOrgId))
        .recoverWith(error => InternalServerError(error.getMessage))
  }

  private[http] val prefixPath = "/organization"

  def routes(authMiddleware: AuthMiddleware[F, StaffAuth]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
