package com.bbr.commerz.sales.domain.order

import cats.effect.Async
import cats.implicits._
import com.bbr.commerz.sales.domain.cart.CartPayloads.CartTotal
import com.bbr.commerz.inventory.domain.product.ProductAlgebra
import com.bbr.commerz.inventory.domain.product.ProductPayloads.Quantity
import com.bbr.commerz.sales.domain.order.OrderPayloads._
import com.bbr.commerz.sales.domain.transaction.TransactionPayloads.{RefundedAmount, TotalAmount}
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Product.ProductId
import com.bbr.platform.domain.Staff.StaffId
import com.bbr.platform.effekts.GenUUID
import com.bbr.platform.getCurrentTime

class OrderService[F[_]: GenUUID: Async](orderRepository: OrderAlgebra[F], productRepository: ProductAlgebra[F]) {

  import OrderService._

  def create(orgId: OrgId, staffId: StaffId, request: OrderRequest): F[Order] =
    for {
      uuid    <- GenUUID[F].make
      created <-
        orderRepository.create(
          request.toDomain(
            id = OrderId(uuid),
            orgId = orgId,
            staffId = staffId,
            createdAt = getCurrentTime.some
          )
        )
    } yield created

  def setReturnedProducts(orgId: OrgId, orderId: OrderId, request: OrderReturn): F[Order] =
    for {
      old      <- getById(orgId, orderId)
      newBody  <- buildReturnedBody(old, request)
      _        <- old.returnedItems
                    .map(
                      _.filter { case (_, returnReason) =>
                        returnReason.reason == ReturnReason.GOODS_STATE
                      }
                        .map { case (productId, returnReason) =>
                          productRepository.increaseProductQuantity(productId, returnReason.quantity)
                        }
                        .toList
                        .sequence
                    )
                    .sequence
      returned <- orderRepository.setReturnedProducts(newBody)
    } yield returned

  def updateById(orgId: OrgId, orderId: OrderId, staffId: StaffId, request: OrderUpdate): F[Order] =
    for {
      old     <- getById(orgId, orderId)
      newBody <- buildUpdateBody(old, staffId, request)
      updated <- orderRepository.updateById(newBody)
    } yield updated

  def updateStatus(orgId: OrgId, orderId: OrderId, status: OrderStatus): F[Unit] =
    status match {
      case OrderStatus.SHIPPED =>
        for {
          order <- orderRepository.getById(orgId, orderId)
          _     <- order.products
                     .filter { case (_, _) => order.orderStatus != OrderStatus.SHIPPED }
                     .map { case (productId, quantity) =>
                       productRepository.decreaseProductQuantity(productId, quantity)
                     }
                     .toList
                     .sequence
          res   <- orderRepository.updateStatus(orgId, orderId, status)
        } yield res

      case OrderStatus.CANCELED =>
        for {
          order <- orderRepository.getById(orgId, orderId)
          _     <- order.products
                     .filter { case (_, _) =>
                       order.orderStatus != OrderStatus.PENDING || order.orderStatus != OrderStatus.CANCELED
                     }
                     .map { case (productId, quantity) =>
                       productRepository.increaseProductQuantity(productId, quantity)
                     }
                     .toList
                     .sequence
          res   <- orderRepository.updateStatus(orgId, orderId, status)
        } yield res

      case _ =>
        orderRepository.updateStatus(orgId, orderId, status)
    }

  def getById(orgId: OrgId, orderId: OrderId): F[Order] =
    orderRepository.getById(orgId, orderId)

  def getAll(orgId: OrgId, limit: Option[Int], offset: Option[Int]): F[List[Order]] =
    orderRepository.getAll(orgId, limit.getOrElse(50), offset.getOrElse(0))

  def createRequest(cartTotal: CartTotal): OrderRequest =
    OrderRequest(
      totalAmount = TotalAmount(cartTotal.total),
      products = cartTotal.products.map(product => product.product.id -> product.quantity).toMap,
      branchId = None,
      deliveryTime = None
    )

  def deleteById(orgId: OrgId, orderId: OrderId): F[Unit] =
    orderRepository.deleteById(orgId, orderId)

}

object OrderService {

  private def buildUpdateBody[F[_]: Async](old: Order, staffId: StaffId, request: OrderUpdate): F[Order] =
    old.orderStatus match {
      case OrderStatus.PENDING =>
        old
          .copy(
            products = request.products.getOrElse(old.products),
            paidAmount = request.paidAmount.getOrElse(old.paidAmount),
            refundedAmount = request.refundedAmount,
            deliveryTime = request.deliveryTime,
            updatedBy = staffId.some,
            updatedAt = getCurrentTime.some
          )
          .pure[F]
      case status              =>
        new Throwable(s"Couldn't update order. Current status: ${status.entryName}").raiseError[F, Order]
    }

  private def buildReturnedBody[F[_]: Async](old: Order, request: OrderReturn): F[Order] = {
    val returnedItems                     = if (request.returnedItems.exists(validateReturnedItems(old.products, _))) {
      request.returnedItems.pure[F]
    } else {
      new Throwable("The number of returned items more than delivered items.")
        .raiseError[F, Option[Map[ProductId, QuantityWithReason]]]
    }
    val amount: F[Option[RefundedAmount]] = if (request.refundedAmount.isDefined) {
      request.refundedAmount.filter(_.value.value <= old.paidAmount.value.value) match {
        case Some(refundedAmount) => refundedAmount.some.pure[F]
        case None                 =>
          new Throwable("The refunded amount more than received amount").raiseError[F, Option[RefundedAmount]]
      }
    } else {
      old.refundedAmount.pure[F]
    }

    (returnedItems, amount).mapN { (items, refAmount) =>
      old
        .copy(
          refundedAmount = refAmount,
          returnedItems = items,
          deliveryTime = request.deliveryTime
        )
    }
  }

  private def validateReturnedItems(
    productsQuantity: Map[ProductId, Quantity],
    returnedItems: Map[ProductId, QuantityWithReason]
  ): Boolean =
    returnedItems.forall { case (productId, returnedQuantity) =>
      productsQuantity.get(productId).exists(_.value >= returnedQuantity.quantity.value)
    }
}
