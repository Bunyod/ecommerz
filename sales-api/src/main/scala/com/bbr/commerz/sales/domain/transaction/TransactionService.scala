package com.bbr.commerz.sales.domain.transaction

import cats.MonadThrow
import cats.implicits._
import com.bbr.commerz.sales.domain.order.OrderPayloads._
import com.bbr.commerz.sales.domain.transaction.TransactionPayloads._
import com.bbr.commerz.sales.domain.transaction.TransactionPayloads.Transaction
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.StaffId
import com.bbr.platform.domain.Transaction.TransactionId
import com.bbr.platform.effekts.GenUUID
import com.bbr.platform.getCurrentTime
import squants.market.{Money, USD}

class TransactionService[F[_]: GenUUID: MonadThrow](transactionRepository: TransactionAlgebra[F]) {

  def create(orgId: OrgId, staffId: StaffId, request: TransactionRequest): F[Transaction] =
    for {
      uuid  <- GenUUID[F].make
      check <- transactionRepository.checkOrganizationExistence(orgId, staffId)
      res   <-
        if (check)
          transactionRepository.create(request.toDomain(TransactionId(uuid), orgId, staffId, getCurrentTime.some))
        else new Throwable(s"The organization not found with ID: ${orgId.value}").raiseError[F, Transaction]
    } yield res

  def getById(orgId: OrgId, transactionId: TransactionId): F[Transaction] =
    transactionRepository.checkTransactionExistence(transactionId).flatMap {
      case true => transactionRepository.getById(orgId, transactionId)
      case _    => new Throwable("The transaction does not exists.").raiseError[F, Transaction]
    }

  def getAll(
    orgId: OrgId,
    orderId: Option[OrderId] = None,
    limit: Option[Int] = None,
    offset: Option[Int] = None
  ): F[List[Transaction]] =
    transactionRepository.getAll(orgId, orderId, limit, offset)

  def deleteById(orgId: OrgId, transactionId: TransactionId): F[String] =
    transactionRepository.checkTransactionExistence(transactionId).flatMap {
      case true => transactionRepository.deleteById(orgId, transactionId)
      case _    => new Throwable("The transaction does not exists.").raiseError[F, String]
    }

}

object TransactionService {

  def computeAmount(
    transaction: Transaction,
    order: Order
  ): Order = {
    val newPaidAmount: BigDecimal   = order.paidAmount.value.amount + transaction.amount.value.amount
    val newUnpaidAmount: BigDecimal = order.unpaidAmount.value.amount - transaction.amount.value.amount

    val newPaymentStatus: PaymentStatus =
      if (order.totalAmount.value.amount == newPaidAmount) PaymentStatus.FULL
      else PaymentStatus.PARTIALLY_PAID

    val newOrderStatus = if (newPaymentStatus == PaymentStatus.FULL) OrderStatus.COMPLETED else order.orderStatus

    order.copy(
      paidAmount = PaidAmount(Money(newPaidAmount, USD)),
      unpaidAmount = UnpaidAmount(Money(newUnpaidAmount, USD)),
      paymentStatus = newPaymentStatus,
      orderStatus = newOrderStatus
    )
  }

}
