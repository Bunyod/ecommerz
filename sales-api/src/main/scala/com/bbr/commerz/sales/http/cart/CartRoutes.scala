package com.bbr.commerz.sales.http.cart

import cats.effect.Async
import cats.implicits._
import com.bbr.commerz.sales.domain.cart.CartPayloads.Cart
import com.bbr.commerz.sales.domain.cart.CartService
import com.bbr.commerz.sales.http.utils.json._
import com.bbr.platform.UuidOpts
import com.bbr.platform.domain.Product._
import com.bbr.platform.domain.Staff.StaffAuth
import com.bbr.platform.utils.decoder.RefinedRequestDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}

class CartRoutes[F[_]: Async](cart: CartService[F]) extends Http4sDsl[F] {

  private val prefixPath = "/org"

  private val httpRoutes: AuthedRoutes[StaffAuth, F] = AuthedRoutes.of {

    case ar @ POST -> Root / UUIDVar(_) / "cart" as staff =>
      ar.req.decodeR[Cart] { c =>
        c.products
          .map { case (id, quantity) =>
            cart.add(staff.id, id, quantity)
          }
          .toList
          .sequence *> Created()
      }

    case GET -> Root / UUIDVar(orgId) / "cart" as staff =>
      Ok(cart.get(orgId.toOrgId, staff.id))

    case ar @ PUT -> Root / UUIDVar(_) / "cart" as staff =>
      ar.req.decodeR[Cart] { c =>
        cart.update(staff.id, c) *> Ok()
      }

    case DELETE -> Root / UUIDVar(_) / UUIDVar(productId) / "cart" as staff =>
      cart.removeProduct(staff.id, ProductId(productId)) *> NoContent()
  }

  def routes(authMiddleware: AuthMiddleware[F, StaffAuth]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
