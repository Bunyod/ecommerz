package com.bbr.commerz.sales.infrastructure.postgres

import cats.effect._
import cats.implicits._
import com.bbr.commerz.organization.domain.staff.StaffPayloads.StaffStatus
import com.bbr.commerz.sales.domain.order.OrderPayloads._
import com.bbr.commerz.sales.domain.transaction.TransactionAlgebra
import com.bbr.commerz.sales.domain.transaction.TransactionPayloads._
import com.bbr.commerz.sales.domain.transaction.TransactionService.computeAmount
import com.bbr.commerz.sales.http.utils.json._
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.StaffId
import com.bbr.platform.domain.Transaction.TransactionId
import doobie._
import doobie.implicits._
import doobie.implicits.javasql._              // DON'T REMOVE IT
import doobie.postgres.implicits._             // DON'T REMOVE IT
import doobie.postgres.circe.jsonb.implicits._ // DON'T REMOVE IT
import doobie.util.transactor.Transactor
import doobie.util.update.Update0
import io.circe.syntax._

import java.sql.Timestamp
import java.util.UUID

class TransactionRepository[F[_]: Async](tr: Transactor[F]) extends TransactionAlgebra[F] {

  import TransactionRepository._

  override def create(transaction: Transaction): F[Transaction] =
    (for {
      _              <- blockOrder(transaction.orgId, transaction.orderId).unique
      order          <- selectOrder(transaction.orgId, transaction.orderId).unique
      newTransaction <- insert(transaction, order).run
      updatedOrder    = computeAmount(transaction, order)
      _              <- updateOrder(updatedOrder).run
    } yield newTransaction).transact(tr) *> getById(transaction.orgId, transaction.id)

  override def getById(orgId: OrgId, transactionId: TransactionId): F[Transaction] =
    selectById(orgId.value, transactionId.value).option.transact(tr).flatMap {
      case Some(transaction) => transaction.pure[F]
      case None              => new Throwable(s"Transaction not found with ID: $transactionId").raiseError[F, Transaction]
    }

  override def deleteById(orgId: OrgId, transactionId: TransactionId): F[String] =
    delete(transactionId.value).run.transact(tr) *> "Successfully deleted.".pure[F]

  override def checkOrganizationExistence(orgId: OrgId, staffId: StaffId): F[Boolean] =
    checkOrg(orgId.value, staffId.value).unique.transact(tr)

  override def checkTransactionExistence(transactionId: TransactionId): F[Boolean] =
    checkTransaction(transactionId.value).unique.transact(tr)

  override def getAll(
    orgId: OrgId,
    orderId: Option[OrderId],
    limit: Option[Int],
    offset: Option[Int]
  ): F[List[Transaction]] =
    orderId match {
      case Some(id) => selectAll(orgId.value, id.value, limit, offset).to[List].transact(tr)
      case None     => selectAll(orgId.value, limit, offset).to[List].transact(tr)
    }

}

object TransactionRepository {

  import Drivers._

  private def blockOrder(orgId: OrgId, orderId: OrderId): Query0[Unit] =
    sql"""SELECT 1 FROM ORDERS WHERE ID = ${orderId.value} AND ORG_ID = ${orgId.value} FOR UPDATE""".query

  private def selectOrder(orgId: OrgId, orderId: OrderId): Query0[Order] =
    sql"""SELECT * FROM ORDERS WHERE ID = ${orderId.value} AND ORG_ID = ${orgId.value}""".query

  private def updateOrder(order: Order): Update0 =
    sql"""
         UPDATE ORDERS SET
         PAID_AMOUNT = ${order.paidAmount.value.amount},
         UNPAID_AMOUNT = ${order.unpaidAmount.value.amount},
         PAYMENT_STATUS = ${order.paymentStatus.entryName},
         ORDER_STATUS = ${order.orderStatus.entryName}
         WHERE ID = ${order.id.value} AND
         ORG_ID = ${order.orgId.value}
       """.update

  private def checkOrg(orgId: UUID, staffId: UUID): Query0[Boolean] =
    sql"""SELECT EXISTS(SELECT * FROM STAFF WHERE (
         ID = $staffId AND
         ORG_ID = $orgId AND
         STATUS = ${StaffStatus.ACTIVE.entryName}
         ))""".query

  private def checkTransaction(transactionId: UUID): Query0[Boolean] =
    sql"""SELECT EXISTS(SELECT * FROM TRANSACTION WHERE ID = $transactionId)""".query

  private def insert(transaction: Transaction, order: Order): Update0 =
    sql"""INSERT INTO TRANSACTION (
          ID,
          ORG_ID,
          STAFF_ID,
          ORDER_ID,
          BRANCH_ID,
          PRODUCTS,
          PAYMENT_TYPE,
          AMOUNT,
          DISCOUNT,
          DEADLINE,
          CREATED_AT,
          UPDATED_AT
          ) VALUES (
         ${transaction.id.value},
         ${transaction.orgId.value},
         ${transaction.staffId.value},
         ${transaction.orderId.value},
         ${transaction.branchId.map(_.value)},
         ${order.products.asJson},
         ${transaction.paymentType.entryName},
         ${transaction.amount.value.amount},
         ${transaction.discount.map(_.value)},
         ${transaction.deadline.map(Timestamp.valueOf)},
         ${transaction.createdAt.map(Timestamp.valueOf)},
         ${transaction.updatedAt.map(Timestamp.valueOf)}
         )
       """.update

  private def selectById(orgId: UUID, transactionId: UUID): Query0[Transaction] =
    sql"""SELECT * FROM TRANSACTION WHERE ID = $transactionId AND ORG_ID = $orgId""".query

  private def selectAll(
    orgId: UUID,
    orderId: UUID,
    limit: Option[Int],
    offset: Option[Int]
  ): Query0[Transaction] =
    sql"""
         SELECT * FROM TRANSACTION
         WHERE ORG_ID = $orgId
         AND ORDER_ID = $orderId
         OFFSET COALESCE($offset, 0) ROWS
         FETCH NEXT COALESCE($limit, 50) ROWS ONLY;
       """.query

  private def selectAll(
    orgId: UUID,
    limit: Option[Int],
    offset: Option[Int]
  ): Query0[Transaction] =
    sql"""
     SELECT * FROM TRANSACTION
     WHERE ORG_ID = $orgId
     OFFSET COALESCE($offset, 0) ROWS
     FETCH NEXT COALESCE($limit, 50) ROWS ONLY;
       """.query

  private def delete(transactionId: UUID): Update0 =
    sql"""DELETE FROM TRANSACTION WHERE ID = $transactionId""".update

}
