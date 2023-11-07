package com.bbr.commerz.sales.domain.transaction

import cats.implicits._
import com.bbr.commerz.inventory.domain.product.ProductPayloads.Quantity
import com.bbr.commerz.sales.domain.order.OrderPayloads.OrderId
import com.bbr.platform.domain.Branch.BranchId
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Product.ProductId
import com.bbr.platform.domain.Staff.StaffId
import com.bbr.platform.domain.Transaction.TransactionId
import com.bbr.platform.getCurrentTime
import enumeratum.{CirceEnum, Enum, EnumEntry}
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import squants.market.Money

import java.time.LocalDateTime

object TransactionPayloads {

  case class TotalAmount(value: Money)
  case class PaidAmount(value: Money)
  case class UnpaidAmount(value: Money)
  case class RefundedAmount(value: Money)

  case class DiscountParam(value: PosInt) {
    def toDomain: Discount = Discount(value.value)
  }
  case class Discount(value: Int)

  case class PaymentTypeParam(paymentType: NonEmptyString) {
    def toDomain: PaymentType = PaymentType.withName(paymentType.value)
  }

  sealed trait PaymentType extends EnumEntry

  object PaymentType extends Enum[PaymentType] with CirceEnum[PaymentType] {
    val values: IndexedSeq[PaymentType] = findValues
    case object CREDIT_CARD extends PaymentType
    case object CASH        extends PaymentType
  }

  case class TransactionRequest(
    orderId: OrderId,
    branchId: Option[BranchId],
    deadline: Option[LocalDateTime],
    paymentType: PaymentType,
    totalAmount: TotalAmount,
    discount: Option[DiscountParam]
  ) {
    def toDomain(
      id: TransactionId,
      orgId: OrgId,
      staffId: StaffId,
      updatedAt: Option[LocalDateTime]
    ): Transaction =
      Transaction(
        id = id,
        orgId = orgId,
        staffId = staffId,
        branchId = branchId,
        orderId = orderId,
        products = Map.empty[ProductId, Quantity],
        paymentType = paymentType,
        amount = totalAmount,
        discount = discount.map(_.toDomain),
        deadline = deadline,
        createdAt = getCurrentTime.some,
        updatedAt = updatedAt
      )
  }

  case class Transaction(
    id: TransactionId,
    orgId: OrgId,
    staffId: StaffId,
    orderId: OrderId,
    branchId: Option[BranchId],
    products: Map[ProductId, Quantity],
    paymentType: PaymentType,
    amount: TotalAmount,
    discount: Option[Discount],
    deadline: Option[LocalDateTime] = None,
    createdAt: Option[LocalDateTime],
    updatedAt: Option[LocalDateTime] = None
  )
}
