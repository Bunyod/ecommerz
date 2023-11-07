package com.bbr.commerz.inventory.http.category

import cats.effect.Async
import cats.implicits._
import com.bbr.commerz.inventory.domain.category.CategoryPayloads._
import com.bbr.commerz.inventory.domain.category.CategoryService
import com.bbr.commerz.inventory.http.utils.json._
import com.bbr.platform.UuidOpts
import com.bbr.platform.domain.Staff.StaffAuth
import com.bbr.platform.domain.Staff.StaffRole.{OWNER, WORKER}
import com.bbr.platform.http.QueryParameters
import com.bbr.platform.utils.decoder._
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe._
import org.http4s.server.{AuthMiddleware, Router}

class CategoryRoutes[F[_]: Async: JsonDecoder](
  categoryService: CategoryService[F]
) extends QueryParameters[F] {

  val httpRoutes: AuthedRoutes[StaffAuth, F] = AuthedRoutes.of {

    case ar @ POST -> Root / UUIDVar(orgId) / "inventory" / "categories" as StaffAuth(_, _, _, OWNER | WORKER) =>
      ar.req.decodeR[CategoryRequest] { category =>
        Created(categoryService.create(orgId.toOrgId, category))
          .recoverWith(error => InternalServerError(error.getMessage))
      }

    case ar @ PUT -> Root / UUIDVar(orgId) / "inventory" / "categories" / UUIDVar(id)
        as StaffAuth(_, _, _, OWNER | WORKER) =>
      ar.req.decodeR[CategoryRequest] { category =>
        Ok(categoryService.updateById(orgId.toOrgId, CategoryId(id), category))
          .recoverWith(error => InternalServerError(error.getMessage))
      }

    case GET -> Root / UUIDVar(orgId) / "inventory" / "categories" / UUIDVar(id) as _ =>
      Ok(categoryService.getById(orgId.toOrgId, CategoryId(id)))
        .recoverWith(error => InternalServerError(error.getMessage))

    case GET -> Root / UUIDVar(orgId) / "inventory" / "categories" :? NameQueryParamMatcher(name) as _ =>
      Ok(categoryService.getAll(orgId.toOrgId, name))
        .recoverWith(error => InternalServerError(error.getMessage))

    case DELETE -> Root / UUIDVar(orgId) / "inventory" / "categories" / UUIDVar(id)
        as StaffAuth(_, _, _, WORKER | OWNER) =>
      Ok(categoryService.deleteById(orgId.toOrgId, CategoryId(id)))
        .recoverWith(error => InternalServerError(error.getMessage))
  }

  private val prefixPath = "/org"

  def routes(authMiddleware: AuthMiddleware[F, StaffAuth]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
