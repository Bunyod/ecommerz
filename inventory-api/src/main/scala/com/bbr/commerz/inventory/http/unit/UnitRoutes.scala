package com.bbr.commerz.inventory.http.unit

import cats.effect.Async
import cats.implicits._
import com.bbr.commerz.inventory.domain.unit.UnitService
import com.bbr.commerz.inventory.domain.unit.UnitPayloads.{UnitId, UnitRequest}
import com.bbr.commerz.inventory.http.utils.json._
import com.bbr.platform.UuidOpts
import com.bbr.platform.domain.Staff.StaffAuth
import com.bbr.platform.domain.Staff.StaffRole.{OWNER, WORKER}
import com.bbr.platform.http.QueryParameters
import com.bbr.platform.utils.decoder._
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe._
import org.http4s.server.{AuthMiddleware, Router}

class UnitRoutes[F[_]: Async: JsonDecoder](
  unitService: UnitService[F]
) extends QueryParameters[F] {

  val httpRoutes: AuthedRoutes[StaffAuth, F] = AuthedRoutes.of {

    case ar @ POST -> Root / UUIDVar(orgId) / "inventory" / "units" as StaffAuth(_, _, _, OWNER | WORKER) =>
      ar.req.decodeR[UnitRequest] { unit =>
        Created(unitService.create(orgId.toOrgId, unit))
          .recoverWith(error => InternalServerError(error.getMessage))
      }

    case ar @ PUT -> Root / UUIDVar(orgId) / "inventory" / "units" / UUIDVar(id)
        as StaffAuth(_, _, _, OWNER | WORKER) =>
      ar.req.decodeR[UnitRequest] { unit =>
        Ok(unitService.updateById(orgId.toOrgId, UnitId(id), unit))
          .recoverWith(error => InternalServerError(error.getMessage))
      }

    case GET -> Root / UUIDVar(orgId) / "inventory" / "units" / UUIDVar(id) as _ =>
      Ok(unitService.getById(orgId.toOrgId, UnitId(id)))
        .recoverWith(error => InternalServerError(error.getMessage))

    case GET -> Root / UUIDVar(orgId) / "inventory" / "units" :? NameQueryParamMatcher(name) as _ =>
      Ok(unitService.getAll(orgId.toOrgId, name))
        .recoverWith(error => InternalServerError(error.getMessage))

    case DELETE -> Root / UUIDVar(orgId) / "inventory" / "units" / UUIDVar(id) as StaffAuth(_, _, _, OWNER | WORKER) =>
      Ok(unitService.deleteById(orgId.toOrgId, UnitId(id)))
        .recoverWith(error => InternalServerError(error.getMessage))
  }

  private val prefixPath = "/org"

  def routes(authMiddleware: AuthMiddleware[F, StaffAuth]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
