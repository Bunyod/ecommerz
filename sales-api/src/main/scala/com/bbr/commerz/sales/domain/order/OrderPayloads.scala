package com.bbr.commerz.sales.domain.order

import com.bbr.commerz.inventory.domain.product.ProductPayloads.Quantity
import com.bbr.commerz.sales.domain.transaction.TransactionPayloads._
import com.bbr.platform.domain.Branch.BranchId
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Product.ProductId
import com.bbr.platform.domain.Staff.StaffId
import enumeratum.{CirceEnum, Enum, EnumEntry}
import squants.market.{Money, USD}

import java.time.LocalDateTime
import java.util.UUID

object OrderPayloads {

  case class OrderId(value: UUID)

  case class OrderRequest(
    totalAmount: TotalAmount,
    products: Map[ProductId, Quantity],
    branchId: Option[BranchId] = None,
    deliveryTime: Option[LocalDateTime]
  ) {
    def toDomain(
      id: OrderId,
      orgId: OrgId,
      staffId: StaffId,
      refundedAmount: Option[RefundedAmount] = None,
      returnedItems: Option[Map[ProductId, QuantityWithReason]] = None,
      updatedBy: Option[StaffId] = None,
      createdAt: Option[LocalDateTime] = None,
      updatedAt: Option[LocalDateTime] = None
    ): Order =
      Order(
        id = id,
        staffId = staffId,
        orgId = orgId,
        branchId = branchId,
        paymentStatus = PaymentStatus.UNPAID,
        orderStatus = OrderStatus.PENDING,
        products = products,
        totalAmount = totalAmount,
        paidAmount = PaidAmount(Money(0.0, USD)),
        unpaidAmount = UnpaidAmount(totalAmount.value),
        createdBy = staffId,
        updatedBy = updatedBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deliveryTime = deliveryTime,
        refundedAmount = refundedAmount,
        returnedItems = returnedItems
      )
  }

  case class OrderReturn(
    refundedAmount: Option[RefundedAmount],
    returnedItems: Option[Map[ProductId, QuantityWithReason]],
    deliveryTime: Option[LocalDateTime]
  )

  case class OrderUpdate(
    products: Option[Map[ProductId, Quantity]],
    paidAmount: Option[PaidAmount],
    refundedAmount: Option[RefundedAmount],
    deliveryTime: Option[LocalDateTime]
  )

  case class Order(
    id: OrderId,
    staffId: StaffId,
    orgId: OrgId,
    branchId: Option[BranchId],
    paymentStatus: PaymentStatus,
    orderStatus: OrderStatus,
    products: Map[ProductId, Quantity],
    totalAmount: TotalAmount,
    paidAmount: PaidAmount,
    unpaidAmount: UnpaidAmount,
    refundedAmount: Option[RefundedAmount],
    returnedItems: Option[Map[ProductId, QuantityWithReason]],
    createdBy: StaffId,
    updatedBy: Option[StaffId],
    createdAt: Option[LocalDateTime],
    updatedAt: Option[LocalDateTime],
    deliveryTime: Option[LocalDateTime]
  )

  sealed trait PaymentStatus extends EnumEntry
  object PaymentStatus       extends Enum[PaymentStatus] with CirceEnum[PaymentStatus] {
    val values: IndexedSeq[PaymentStatus] = findValues
    case object PARTIALLY_PAID extends PaymentStatus
    case object FULL           extends PaymentStatus
    case object UNPAID         extends PaymentStatus
  }

  sealed trait OrderStatus extends EnumEntry
  object OrderStatus       extends Enum[OrderStatus] with CirceEnum[OrderStatus] {
    val values: IndexedSeq[OrderStatus] = findValues
    case object PENDING    extends OrderStatus
    case object CONFIRMED  extends OrderStatus
    case object PROCESSING extends OrderStatus
    case object SHIPPED    extends OrderStatus
    case object COMPLETED  extends OrderStatus
    case object CANCELED   extends OrderStatus
    case object RETURN     extends OrderStatus
  }

  sealed trait ReturnReason extends EnumEntry
  object ReturnReason       extends Enum[ReturnReason] with CirceEnum[ReturnReason] {
    val values: IndexedSeq[ReturnReason] = findValues
    case object GOODS_STATE extends ReturnReason
    case object EXPIRED     extends ReturnReason
  }

  case class QuantityWithReason(quantity: Quantity, reason: ReturnReason)

}
