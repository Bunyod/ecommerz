package com.bbr.commerz.sales.http.cart

import cats.data.Kleisli
import cats.implicits._
import cats.effect.IO
import com.bbr.commerz.inventory.domain.product.ProductPayloads.Quantity
import com.bbr.commerz.organization.suite.HttpTestSuite
import com.bbr.commerz.sales.domain.cart.{CartAlgebra, CartService}
import com.bbr.commerz.sales.domain.cart.CartPayloads._
import com.bbr.commerz.sales.suite.SaleGenerators
import com.bbr.commerz.sales.suite.json._
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Product.ProductId
import com.bbr.platform.domain.Staff.{StaffAuth, StaffId}
import io.circe.syntax.EncoderOps
import org.http4s.server.AuthMiddleware
import org.http4s.Method._
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.headers.`Content-Type`
import squants.market.{Money, USD}

import java.util.UUID

object CartRoutesSpec extends HttpTestSuite with SaleGenerators {

  def authMiddleware(staffAuth: StaffAuth): AuthMiddleware[IO, StaffAuth] =
    AuthMiddleware(Kleisli.pure(staffAuth))

  def dataCart(cartTotal: CartTotal): CartService[IO] =
    new CartService[IO](new TestCartRepository {
      override def get(orgId: OrgId, staffId: StaffId): IO[CartTotal] = IO.pure(cartTotal)
    })

  test("GET cart [OK]") {
    forall(genStaffAuth) { staff =>
      val total  = genCartTotal.sample.get
      val req    = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/cart"))
      val routes = new CartRoutes[IO](dataCart(total)).routes(authMiddleware(staff))
      expectHttpBodyAndStatus(routes, req)(total, Status.Ok)
    }
  }

  test("POST add product to cart [OK]") {
    forall(genStaffAuth) { staff =>
      val total       = genCart.sample.get
      val req         = POST(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/cart"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(total.asJson)
      val cartService = new CartService[IO](new TestCartRepository)
      val routes      = new CartRoutes[IO](cartService).routes(authMiddleware(staff))
      expectHttpStatus(routes, req)(Status.Created)
    }
  }
}

protected[http] class TestCartRepository extends CartAlgebra[IO] {
  override def add(
    staffId: StaffId,
    productId: ProductId,
    quantity: Quantity
  ): IO[Unit] = ().pure[IO]

  override def get(orgId: OrgId, staffId: StaffId): IO[CartTotal] =
    CartTotal(List(), Money(9.3, USD)).pure[IO]

  override def delete(staffId: StaffId): IO[Unit] = ().pure[IO]

  override def removeProduct(staffId: StaffId, productId: ProductId): IO[Unit] = ().pure[IO]

  override def update(staffId: StaffId, cart: Cart): IO[Unit] = ().pure[IO]
}
