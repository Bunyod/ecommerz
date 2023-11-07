package com.bbr.commerz.sales.infrastructure.postgres

import cats.effect.Sync
import cats.implicits._
import com.bbr.commerz.sales.domain.order.OrderPayloads._
import com.bbr.commerz.sales.domain.order.OrderAlgebra
import com.bbr.commerz.sales.http.utils.json._
import com.bbr.platform.domain.Organization.OrgId
import doobie._
import doobie.implicits._
import doobie.implicits.javasql._              // DON'T REMOVE IT
import doobie.postgres.implicits._             // DON'T REMOVE IT
import doobie.postgres.circe.jsonb.implicits._ // DON'T REMOVE IT
import doobie.util.update.Update0
import io.circe.syntax._

import java.sql.Timestamp

class OrderRepository[F[_]: Sync](tr: Transactor[F]) extends OrderAlgebra[F] {

  import OrderRepository._

  override def create(order: Order): F[Order] =
    insert(order).run.transact(tr) *> getById(order.orgId, order.id)

  override def setReturnedProducts(order: Order): F[Order] =
    returnOrder(order).run.transact(tr) *> getById(order.orgId, order.id)

  override def updateById(order: Order): F[Order] =
    update(order).run.transact(tr) *> getById(order.orgId, order.id)

  override def updateStatus(orgId: OrgId, orderId: OrderId, status: OrderStatus): F[Unit] =
    updateSts(orgId, orderId, status).run.transact(tr).void

  override def getById(orgId: OrgId, orderId: OrderId): F[Order] =
    select(orgId, orderId).option.transact(tr).flatMap {
      case Some(order) => order.pure[F]
      case None        =>
        new Throwable(s"The order not found. ID: ${orderId.value}; OrgId: ${orgId.value}").raiseError[F, Order]
    }

  override def getAll(orgId: OrgId, limit: Int, offset: Int): F[List[Order]] =
    selectAll(orgId, limit, offset).to[List].transact(tr)

  override def deleteById(orgId: OrgId, orderId: OrderId): F[Unit] =
    delete(orgId, orderId).run.transact(tr).void
}

object OrderRepository {

  import Drivers._

  private def insert(order: Order): Update0 =
    sql"""
         INSERT INTO ORDERS(
           ID,
           STAFF_ID,
           ORG_ID,
           BRANCH_ID,
           PAYMENT_STATUS,
           ORDER_STATUS,
           PRODUCTS,
           TOTAL_AMOUNT,
           PAID_AMOUNT,
           UNPAID_AMOUNT,
           REFUNDED_AMOUNT,
           RETURNED_ITEMS,
           CREATED_BY,
           UPDATED_BY,
           CREATED_AT,
           UPDATED_AT,
           DELIVERY_TIME
         ) VALUES (
           ${order.id.value},
           ${order.staffId.value},
           ${order.orgId.value},
           ${order.branchId.map(_.value)},
           ${order.paymentStatus.entryName},
           ${order.orderStatus.entryName},
           ${order.products.asJson},
           ${order.totalAmount.value.amount},
           ${order.paidAmount.value.amount},
           ${order.unpaidAmount.value.amount},
           ${order.refundedAmount.map(_.value.amount)},
           ${order.returnedItems.map(_.asJson)},
           ${order.createdBy.value},
           ${order.updatedBy.map(_.value)},
           ${order.createdAt.map(Timestamp.valueOf)},
           ${order.updatedAt.map(Timestamp.valueOf)},
           ${order.deliveryTime.map(Timestamp.valueOf)}
         )
       """.update

  private def returnOrder(order: Order): Update0 =
    sql"""
         UPDATE ORDERS SET
         REFUNDED_AMOUNT = ${order.refundedAmount.map(_.value.amount)},
         RETURNED_ITEMS = ${order.returnedItems.map(_.asJson)},
         DELIVERY_TIME = ${order.deliveryTime.map(Timestamp.valueOf)},
         ORDER_STATUS = ${OrderStatus.RETURN.entryName}
         WHERE ID = ${order.id.value} AND
         ORG_ID = ${order.orgId.value}
       """.update

  private def update(order: Order): Update0 =
    sql"""
         UPDATE ORDERS SET
         REFUNDED_AMOUNT = ${order.refundedAmount.map(_.value.amount)},
         PRODUCTS = ${order.products.asJson},
         DELIVERY_TIME = ${order.deliveryTime.map(Timestamp.valueOf)},
         UPDATED_BY = ${order.updatedBy.map(_.value)}
         WHERE ID = ${order.id.value} AND
         ORG_ID = ${order.orgId.value}
       """.update

  private def updateSts(orgId: OrgId, orderId: OrderId, status: OrderStatus): Update0 =
    sql"""
         UPDATE ORDERS SET
         ORDER_STATUS = ${status.entryName}
         WHERE ID = ${orderId.value} AND
         ORG_ID = ${orgId.value}
       """.update

  private def select(orgId: OrgId, orderId: OrderId): Query0[Order] =
    sql"""
         SELECT * FROM ORDERS
         WHERE ID = ${orderId.value} AND
         ORG_ID = ${orgId.value}
       """.query

  private def selectAll(orgId: OrgId, limit: Int, offset: Int): Query0[Order] =
    sql"""
         SELECT * FROM ORDERS
         WHERE ORG_ID = ${orgId.value}
         LIMIT $limit
         OFFSET $offset
       """.query

  private def delete(orgId: OrgId, orderId: OrderId): Update0 =
    sql"""DELETE FROM ORDERS WHERE ID = ${orderId.value} AND ORG_ID = ${orgId.value}""".update

}
