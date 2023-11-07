package com.bbr.commerz.organization.http.branch

import cats.effect.Async
import cats.implicits._
import com.bbr.commerz.organization.domain.branch.BranchService
import com.bbr.commerz.organization.domain.branch.BranchPayloads.{BranchName, BranchRequest, BranchUpdate}
import com.bbr.platform.utils.decoder._
import com.bbr.commerz.organization.http.utils.json._
import com.bbr.platform.UuidOpts
import com.bbr.platform.domain.Branch.BranchId
import com.bbr.platform.domain.Staff.StaffAuth
import com.bbr.platform.domain.Staff.StaffRole.OWNER
import com.bbr.platform.http.QueryParameters
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.server.{AuthMiddleware, Router}

class BranchRoutes[F[_]: Async](
  branchService: BranchService[F]
) extends QueryParameters[F] {

  private val httpRoutes: AuthedRoutes[StaffAuth, F] = AuthedRoutes.of {

    case ar @ POST -> Root / UUIDVar(orgId) / "branches" as _ =>
      ar.req.decodeR[BranchRequest] { branch =>
        Created(branchService.create(orgId.toOrgId, branch, ar.context.id))
          .recoverWith(error => InternalServerError(error.getMessage))
      }

    case ar @ PUT -> Root / UUIDVar(orgId) / "branches" / UUIDVar(branchId) as _ =>
      ar.req.decodeR[BranchUpdate] { branch =>
        Ok(branchService.updateById(orgId.toOrgId, BranchId(branchId), branch))
          .recoverWith(error => InternalServerError(error.getMessage))
      }

    case GET -> Root / UUIDVar(orgId) / "branches" / UUIDVar(branchId) as _ =>
      Ok(branchService.getById(orgId.toOrgId, BranchId(branchId)))
        .recoverWith(error => InternalServerError(error.getMessage))

    case GET -> Root / UUIDVar(orgId) / "branches" :? NameQueryParamMatcher(name) as StaffAuth(_, _, _, OWNER) =>
      Ok(branchService.getAll(orgId.toOrgId, name.map(BranchName.apply)))
        .recoverWith(error => InternalServerError(error.getMessage))

    case DELETE -> Root / UUIDVar(orgId) / "branches" / UUIDVar(branchId) as _ =>
      Ok(branchService.deleteById(orgId.toOrgId, BranchId(branchId)))
        .recoverWith(error => InternalServerError(error.getMessage))
  }

  private[http] val prefixPath = "/org"

  def routes(authMiddleware: AuthMiddleware[F, StaffAuth]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
