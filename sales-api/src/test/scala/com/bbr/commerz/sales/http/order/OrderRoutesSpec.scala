package com.bbr.commerz.sales.http.order

import cats.data.Kleisli
import cats.effect.IO
import cats.implicits._
import com.bbr.commerz.inventory.domain.product.ProductPayloads
import com.bbr.commerz.inventory.http.product.TestProductRepository
import com.bbr.commerz.organization.suite.HttpTestSuite
import com.bbr.commerz.sales.domain.cart.CartService
import com.bbr.commerz.sales.domain.order.OrderPayloads.{Order, OrderId, OrderStatus, QuantityWithReason, ReturnReason}
import com.bbr.commerz.sales.domain.order.{OrderAlgebra, OrderService}
import com.bbr.commerz.sales.http.cart.TestCartRepository
import com.bbr.commerz.sales.suite.SaleGenerators
import com.bbr.commerz.sales.suite.json._
import com.bbr.platform.UuidOpts
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.commerz.sales.domain.transaction.TransactionPayloads.RefundedAmount
import com.bbr.platform.domain.Product
import com.bbr.platform.domain.Staff.{PhoneNumber, StaffAuth, StaffRole}
import io.circe.syntax._
import org.http4s.Method._
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.headers.`Content-Type`
import org.http4s.server.AuthMiddleware

import java.util.UUID

object OrderRoutesSpec extends HttpTestSuite with SaleGenerators {

  def authMiddleware(staffAuth: StaffAuth): AuthMiddleware[IO, StaffAuth] =
    AuthMiddleware(Kleisli.pure(staffAuth))

  val cartService = new CartService[IO](new TestCartRepository)

  val staffAuth: StaffAuth = StaffAuth(
    id = UUID.randomUUID().toStaffId,
    branchId = UUID.randomUUID().toBranchId.some,
    phoneNumber = PhoneNumber("+995489665544"),
    role = StaffRole.WORKER
  )

  val staffOwnerAuth: StaffAuth = StaffAuth(
    id = UUID.randomUUID().toStaffId,
    branchId = UUID.randomUUID().toBranchId.some,
    phoneNumber = PhoneNumber("+995489665544"),
    role = StaffRole.OWNER
  )

  def createOrder(newOrder: Order): OrderService[IO] = new OrderService[IO](
    new TestOrderRepository {
      override def create(order: Order): IO[Order] = newOrder.pure[IO]
    },
    new TestProductRepository
  )

  def failingCreate(): OrderService[IO] = new OrderService[IO](
    new TestOrderRepository {
      override def create(order: Order): IO[Order] = IO.raiseError(DummyError)
    },
    new TestProductRepository
  )

  def updateReturnedItems(newOrder: Order): OrderService[IO] = new OrderService[IO](
    new TestOrderRepository   {
      override def getById(orgId: OrgId, orderId: OrderId): IO[Order] = IO.pure(newOrder)
      override def setReturnedProducts(order: Order): IO[Order]       = IO.pure(newOrder)
    },
    new TestProductRepository {}
  )

  def failingUpdateReturnedItems(): OrderService[IO] = new OrderService[IO](
    new TestOrderRepository   {
      override def getById(orgId: OrgId, orderId: OrderId): IO[Order] = IO.raiseError(DummyError)

      override def setReturnedProducts(order: Order): IO[Order] = IO.raiseError(DummyError)
    },
    new TestProductRepository {}
  )

  def updateOrder(newOrder: Order): OrderService[IO] = new OrderService[IO](
    new TestOrderRepository   {
      override def getById(orgId: OrgId, orderId: OrderId): IO[Order] = IO.pure(newOrder)
      override def updateById(order: Order): IO[Order]                = IO.pure(newOrder)
    },
    new TestProductRepository {}
  )

  def failingUpdate(): OrderService[IO] = new OrderService[IO](
    new TestOrderRepository   {
      override def getById(orgId: OrgId, orderId: OrderId): IO[Order] = IO.raiseError(DummyError)

      override def updateById(order: Order): IO[Order] = IO.raiseError(DummyError)
    },
    new TestProductRepository {}
  )

  def getOrder(order: Order): OrderService[IO] = new OrderService[IO](
    new TestOrderRepository   {
      override def getById(orgId: OrgId, orderId: OrderId): IO[Order] = order.pure[IO]
    },
    new TestProductRepository {}
  )

  def failingGetOrder(): OrderService[IO] = new OrderService[IO](
    new TestOrderRepository   {
      override def getById(orgId: OrgId, orderId: OrderId): IO[Order] = IO.raiseError(DummyError)
    },
    new TestProductRepository {}
  )

  def getAll(orders: List[Order]): OrderService[IO] = new OrderService[IO](
    new TestOrderRepository   {
      override def getAll(orgId: OrgId, limit: Int, offset: Int): IO[List[Order]] = orders.pure[F]
    },
    new TestProductRepository {}
  )

  def failingGetAll(): OrderService[IO] = new OrderService[IO](
    new TestOrderRepository   {
      override def getAll(orgId: OrgId, limit: Int, offset: Int): IO[List[Order]] = IO.raiseError(DummyError)
    },
    new TestProductRepository {}
  )

  def delete(): OrderService[IO] = new OrderService[IO](
    new TestOrderRepository   {
      override def deleteById(orgId: OrgId, orderId: OrderId): IO[Unit] = ().pure[IO]
    },
    new TestProductRepository {}
  )

  def failingDelete(): OrderService[IO] = new OrderService[IO](
    new TestOrderRepository   {
      override def deleteById(orgId: OrgId, orderId: OrderId): IO[Unit] = IO.raiseError(DummyError)
    },
    new TestProductRepository {}
  )

  def updateStatus(order: Order): OrderService[IO] = new OrderService[IO](
    new TestOrderRepository   {
      override def getById(orgId: OrgId, orderId: OrderId): IO[Order]                          = order.pure[IO]
      override def updateStatus(orgId: OrgId, orderId: OrderId, status: OrderStatus): IO[Unit] = ().pure[IO]
    },
    new TestProductRepository {
      override def increaseProductQuantity(productId: Product.ProductId, quantity: ProductPayloads.Quantity): IO[Unit] =
        ().pure[IO]

      override def decreaseProductQuantity(productId: Product.ProductId, quantity: ProductPayloads.Quantity): IO[Unit] =
        ().pure[IO]
    }
  )

  test("create order by StaffId [OK]") {
    forall(genOrder) { order =>
      val orderRequest = genOrderRequest.sample.get
      val req          = POST(Uri.unsafeFromString(s"/org/${order.orgId.value}/order"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(orderRequest.asJson)
      val routes       = new OrderRoutes[IO](createOrder(order), cartService).routes(authMiddleware(staffAuth))
      expectHttpBodyAndStatus(routes, req)(order, Status.Ok)
    }
  }

  test("create order by StaffId, bad request [FAILURE]") {
    forall(genOrder) { order =>
      val orderRequest = genOrderRequest.sample.get
      val req          = POST(Uri.unsafeFromString(s"/org/${order.orgId.value}/order"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(orderRequest.asJson)
      val routes       = new OrderRoutes[IO](failingCreate(), cartService).routes(authMiddleware(staffAuth))
      expectHttpFailure(routes, req)
    }
  }

  test("return order by id [OK]") {
    forall(genOrder) { order =>
      val orderReturn = genOrderReturn.sample.get.copy(
        returnedItems = order.products.map { case (productId, quantity) =>
          productId -> QuantityWithReason(quantity, ReturnReason.EXPIRED)
        }.some,
        refundedAmount = RefundedAmount(order.paidAmount.value).some
      )

      val req                    = PUT(Uri.unsafeFromString(s"/org/${order.orgId.value}/order/${order.id.value}/return"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(orderReturn.asJson)
      val routes: HttpRoutes[IO] =
        new OrderRoutes[IO](updateReturnedItems(order), cartService).routes(authMiddleware(staffAuth))
      expectHttpBodyAndStatus(routes, req)(order, Status.Ok)
    }
  }

  test("return order bad request [FAILURE]") {
    forall(genOrder) { order =>
      val orderReturn            = genOrderReturn.sample.get
      val req                    = PUT(Uri.unsafeFromString(s"/org/${order.orgId.value}/order/${order.id.value}"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(orderReturn.asJson)
      val routes: HttpRoutes[IO] =
        new OrderRoutes[IO](failingUpdateReturnedItems(), cartService).routes(authMiddleware(staffAuth))
      expectHttpFailure(routes, req)
    }
  }

  test("update order by id [OK]") {
    forall(genOrder) { order =>
      val copied      = order.copy(orderStatus = OrderStatus.PENDING)
      val orderUpdate = genOrderUpdate.sample.get

      val req                    = PUT(Uri.unsafeFromString(s"/org/${order.orgId.value}/order/${order.id.value}"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(orderUpdate.asJson)
      val routes: HttpRoutes[IO] =
        new OrderRoutes[IO](updateOrder(copied), cartService)
          .routes(authMiddleware(staffAuth))
      expectHttpBodyAndStatus(routes, req)(copied, Status.Ok)
    }
  }

  test("update order bad request [FAILURE]") {
    forall(genOrder) { _ =>
      val orderUpdate            = genOrderUpdate.sample.get
      val req                    = PUT(Uri.unsafeFromString(s"/wrong"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(orderUpdate.asJson)
      val routes: HttpRoutes[IO] = new OrderRoutes[IO](failingUpdate(), cartService).routes(authMiddleware(staffAuth))
      expectHttpFailure(routes, req)
    }
  }

  test("update status [OK]") {
    forall(genOrder) { order =>
      val statusUpdate           = genOrderStatus.sample.get
      val req                    = PUT(Uri.unsafeFromString(s"/org/${order.orgId.value}/order/${order.id.value}/status"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(statusUpdate.asJson)
      val routes: HttpRoutes[IO] =
        new OrderRoutes[IO](updateStatus(order), cartService).routes(authMiddleware(staffAuth))
      val status                 = s"The order status successfully updated to ${statusUpdate.entryName}"
      expectHttpBodyAndStatus(routes, req)(status, Status.Ok)
    }
  }

  test("get order [OK]") {
    forall(genOrder) { order =>
      val req                    = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/order/${UUID.randomUUID()}"))
      val routes: HttpRoutes[IO] = new OrderRoutes[IO](getOrder(order), cartService).routes(authMiddleware(staffAuth))
      expectHttpBodyAndStatus(routes, req)(order, Status.Ok)
    }
  }

  test("get order [FAILURE]") {
    forall(genOrder) { _ =>
      val req                    = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/order/${UUID.randomUUID()}"))
      val routes: HttpRoutes[IO] = new OrderRoutes[IO](failingGetOrder(), cartService).routes(authMiddleware(staffAuth))
      expectHttpFailure(routes, req)
    }
  }

  test("get order bad request [FAILURE]") {
    forall(genOrder) { _ =>
      val req                    = GET(Uri.unsafeFromString("/wrong"))
      val routes: HttpRoutes[IO] = new OrderRoutes[IO](failingGetOrder(), cartService).routes(authMiddleware(staffAuth))
      expectHttpFailure(routes, req)
    }
  }

  test("get orders by staffId [OK]") {
    forall(genOrders) { orders =>
      val req                    = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/order"))
      val routes: HttpRoutes[IO] = new OrderRoutes[IO](getAll(orders), cartService).routes(authMiddleware(staffAuth))
      expectHttpBodyAndStatus(routes, req)(orders, Status.Ok)
    }
  }

  test("get orders by staffId [FAILURE]") {
    forall(genOrgId) { orgId =>
      val req                    = GET(Uri.unsafeFromString(s"/org/${orgId.value}/order"))
      val routes: HttpRoutes[IO] = new OrderRoutes[IO](failingGetAll(), cartService).routes(authMiddleware(staffAuth))
      expectHttpFailure(routes, req)
    }
  }

  test("get orders by staffId bad request [FAILURE]") {
    forall(genOrgId) { orgId =>
      val req                    = GET(Uri.unsafeFromString(s"/wrong/org/${orgId.value}"))
      val routes: HttpRoutes[IO] = new OrderRoutes[IO](failingGetAll(), cartService).routes(authMiddleware(staffAuth))
      expectHttpFailure(routes, req)
    }
  }

  test("delete order [OK]") {
    forall(genOrgId) { orgId =>
      val req                    = DELETE(Uri.unsafeFromString(s"/org/${orgId.value}/order/${UUID.randomUUID()}"))
      val routes: HttpRoutes[IO] = new OrderRoutes[IO](delete(), cartService).routes(authMiddleware(staffOwnerAuth))
      expectHttpStatus(routes, req)(Status.NoContent)
    }
  }

  test("delete order [FAILURE]") {
    forall(genOrgId) { orgId =>
      val req                    = DELETE(Uri.unsafeFromString(s"/org/${orgId.value}/order/${UUID.randomUUID()}"))
      val routes: HttpRoutes[IO] =
        new OrderRoutes[IO](failingDelete(), cartService).routes(authMiddleware(staffOwnerAuth))
      expectHttpFailure(routes, req)
    }
  }
}

protected[http] class TestOrderRepository extends OrderAlgebra[IO] with SaleGenerators {
  override def create(order: Order): IO[Order] = order.pure[IO]

  override def setReturnedProducts(order: Order): IO[Order] = order.pure[IO]

  override def updateStatus(orgId: OrgId, orderId: OrderId, status: OrderStatus): IO[Unit] = ().pure[IO]

  override def getById(orgId: OrgId, orderId: OrderId): IO[Order] = IO.pure(null)

  override def getAll(orgId: OrgId, limit: Int, offset: Int): IO[List[Order]] = List.empty[Order].pure[IO]

  override def deleteById(orgId: OrgId, orderId: OrderId): IO[Unit] = ().pure[IO]

  override def updateById(order: Order): IO[Order] = order.pure[IO]
}
