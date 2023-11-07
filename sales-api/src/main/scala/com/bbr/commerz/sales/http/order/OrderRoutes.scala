package com.bbr.commerz.sales.http.order

import cats.effect.Async
import cats.implicits._
import com.bbr.commerz.sales.domain.cart.CartService
import com.bbr.commerz.sales.domain.order.OrderPayloads.{OrderId, OrderRequest, OrderReturn, OrderStatus, OrderUpdate}
import com.bbr.commerz.sales.domain.order.OrderService
import com.bbr.commerz.sales.http.utils.json._
import com.bbr.platform.UuidOpts
import com.bbr.platform.domain.Staff.StaffAuth
import com.bbr.platform.domain.Staff.StaffRole.{AGENT, OWNER}
import com.bbr.platform.http.QueryParameters
import com.bbr.platform.utils.decoder.RefinedRequestDecoder
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}

final case class OrderRoutes[F[_]: Async](
  service: OrderService[F],
  cartService: CartService[F]
) extends QueryParameters[F] {

  private val prefixPath = "/org"

  private val httpRoutes: AuthedRoutes[StaffAuth, F] = AuthedRoutes.of {
    case ar @ POST -> Root / UUIDVar(orgId) / "order" as StaffAuth(staffId, _, _, _) =>
      ar.req.decodeR[OrderRequest] { request =>
        Ok(service.create(orgId.toOrgId, staffId, request))
          .recoverWith(error => InternalServerError(error.getMessage))
      }

    case POST -> Root / UUIDVar(orgId) / "order" / "cart" as StaffAuth(agentId, _, _, AGENT) =>
      cartService
        .get(orgId.toOrgId, agentId)
        .flatMap { cartTotal =>
          Ok(service.create(orgId.toOrgId, agentId, service.createRequest(cartTotal)) *> cartService.delete(agentId))
        }
        .recoverWith(error => InternalServerError(error.getMessage))

    case ar @ PUT -> Root / UUIDVar(orgId) / "order" / UUIDVar(id) / "return" as _ =>
      ar.req.decodeR[OrderReturn] { request =>
        Ok(service.setReturnedProducts(orgId.toOrgId, OrderId(id), request))
          .recoverWith(error => InternalServerError(error.getMessage))
      }

    case ar @ PUT -> Root / UUIDVar(orgId) / "order" / UUIDVar(id) as staff =>
      ar.req.decodeR[OrderUpdate] { request =>
        Ok(service.updateById(orgId.toOrgId, OrderId(id), staff.id, request))
          .recoverWith(error => InternalServerError(error.getMessage))
      }

    case ar @ PUT -> Root / UUIDVar(orgId) / "order" / UUIDVar(id) / "status" as _ =>
      ar.req.decodeR[OrderStatus] { status =>
        (service.updateStatus(orgId.toOrgId, OrderId(id), status) *>
          Ok(s"The order status successfully updated to ${status.entryName}"))
          .recoverWith(error => InternalServerError(error.getMessage))
      }

    case GET -> Root / UUIDVar(orgId) / "order" / UUIDVar(id) as _ =>
      Ok(service.getById(orgId.toOrgId, OrderId(id)))
        .recoverWith(error => InternalServerError(error.getMessage))

    case GET -> Root / UUIDVar(orgId) / "order" :? LimitQueryParamMatcher(
          limit
        ) +& OffsetQueryParamMatcher(
          offset
        ) as _ =>
      Ok(service.getAll(orgId.toOrgId, limit, offset))

    case DELETE -> Root / UUIDVar(orgId) / "order" / UUIDVar(id) as StaffAuth(_, _, _, OWNER) =>
      (Ok(service.deleteById(orgId.toOrgId, OrderId(id))) *> NoContent())
        .recoverWith(error => InternalServerError(error.getMessage))

  }

  def routes(authMiddleware: AuthMiddleware[F, StaffAuth]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
