package com.bbr.commerz.sales.suite

import cats.{Eq, Show}
import com.bbr.commerz.inventory.http.utils.json._
import com.bbr.commerz.auth.http.utils.json._
import com.bbr.commerz.inventory.domain.product.ProductPayloads.{CartProduct, Quantity}
import com.bbr.commerz.organization.http.utils.json._
import com.bbr.commerz.sales.domain.agent.AgentPayloads.{AgentClient, AgentClientRequest, AgentClientUpdate, ClientId}
import com.bbr.commerz.sales.domain.cart.CartPayloads.{Cart, CartTotal}
import com.bbr.commerz.sales.domain.order.OrderPayloads._
import com.bbr.commerz.sales.domain.transaction.TransactionPayloads._
import com.bbr.platform.domain.Product.ProductId
import com.bbr.platform.domain.Staff.{StaffAuth, StaffId}
import com.bbr.platform.domain.Transaction.TransactionId
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import io.circe.generic.semiauto._
import io.circe.syntax.EncoderOps
import org.http4s.EntityEncoder
import org.http4s.circe._
import squants.market.Money

object json extends JsonCodecs {
  implicit def deriveEntityEncoder[F[_], A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
}

trait JsonCodecs {

  // ----- Coercible codecs -----
  implicit def coercibleEncoder[A, B: Encoder]: Encoder[A] =
    Encoder[B].contramap[A](_.asInstanceOf[B])

  implicit def coercibleKeyEncoder[A, B: KeyEncoder]: KeyEncoder[A] =
    KeyEncoder[B].contramap[A](_.asInstanceOf[B])

  implicit val customConfig: Configuration =
    Configuration.default.withDefaults.withSnakeCaseMemberNames.withSnakeCaseConstructorNames

  implicit val paymentTypeEncoder: Encoder[PaymentType] = deriveConfiguredEncoder[PaymentType]

  implicit val transactionIdEncoder: Encoder[TransactionId] = deriveEncoder[TransactionId]

  implicit val cartTotalEncoder: Encoder[CartTotal]     = deriveEncoder[CartTotal]
  implicit val cartProductEncoder: Encoder[CartProduct] = deriveEncoder[CartProduct]

  implicit val cartEncoder: Encoder[Cart] = deriveEncoder[Cart]

  implicit val mapEncoder: Encoder[Map[ProductId, Quantity]] =
    Encoder.instance { map =>
      Json.fromJsonObject(JsonObject.fromIterable(map.map { case (key, value) =>
        key.value.toString -> value.value.asJson
      }))
    }

  implicit val orderIdEncoder: Encoder[OrderId] = deriveEncoder[OrderId]

  implicit val productIdEncoder: Encoder[ProductId] = deriveEncoder[ProductId]

  implicit val quantityEncoder: Encoder[Quantity] = Encoder.forProduct1("quantity")(_.value)

  implicit val staffIdEncoder: Encoder[StaffId] =
    Encoder.forProduct1("staff_id")(_.value)

  implicit val moneyEncoder: Encoder[Money] =
    Encoder[BigDecimal].contramap(_.amount)

  implicit val orderEncoder: Encoder[Order] =
    Encoder.forProduct17(
      "id",
      "staff_id",
      "org_id",
      "branch_id",
      "payment_status",
      "order_status",
      "products",
      "total_amount",
      "paid_amount",
      "unpaid_amount",
      "refunded_amount",
      "returned_items",
      "created_by",
      "updated_by",
      "created_at",
      "updated_at",
      "delivery_time"
    )(order =>
      (
        order.id.value,
        order.staffId.value,
        order.orgId.value,
        order.branchId.map(_.value),
        order.paymentStatus.entryName,
        order.orderStatus.entryName,
        order.products.map(v => (v._1.value, v._2.value)),
        order.totalAmount.value.amount,
        order.paidAmount.value.amount,
        order.unpaidAmount.value.amount,
        order.refundedAmount.map(_.value.amount),
        order.returnedItems.map(_.map(i => (i._1.value, (i._2.quantity.value, i._2.reason.entryName)))),
        order.createdBy.value,
        order.updatedBy.map(_.value),
        order.createdAt,
        order.updatedAt,
        order.deliveryTime
      )
    )

  implicit val withReasonEncoder: Encoder[QuantityWithReason] =
    Encoder.forProduct2("quantity", "reason")(r => (r.quantity.value, r.reason.entryName))

  implicit val mapReasonEncoder: Encoder[Map[ProductId, QuantityWithReason]] =
    Encoder.instance { map =>
      Json.fromJsonObject(JsonObject.fromIterable(map.map { case (key, value) =>
        key.value.toString -> withReasonEncoder(value)
      }))
    }

  implicit val orderUpdateEncoder: Encoder[OrderUpdate] = Encoder.forProduct4(
    "products",
    "paid_amount",
    "refunded_amount",
    "delivery_time"
  )(ou =>
    (
      ou.products.map(_.map(v => (v._1.value, v._2.value))),
      ou.paidAmount.map(_.value.amount),
      ou.refundedAmount.map(_.value.amount),
      ou.deliveryTime
    )
  )

  implicit val orderReturnEncoder: Encoder[OrderReturn] = Encoder.forProduct3(
    "refunded_amount",
    "returned_items",
    "delivery_time"
  )(order =>
    (
      order.refundedAmount.map(_.value.amount),
      order.returnedItems.map(i => mapReasonEncoder(i)),
      order.deliveryTime
    )
  )

  implicit val orderStatusEncoder: Encoder[OrderStatus] =
    Encoder.forProduct1("order_status")(_.entryName)

  implicit val agentClientRequestEncoder: Encoder[AgentClientRequest] = Encoder.forProduct4(
    "phone_number",
    "first_name",
    "last_name",
    "address"
  )(agent =>
    (
      agent.phoneNumber.value.value,
      agent.firstName.value.value,
      agent.lastName.value.value,
      agent.address.asJson
    )
  )

  implicit val agentClientEncoder: Encoder[AgentClient] =
    Encoder.forProduct12(
      "id",
      "agent_id",
      "org_id",
      "phone_number",
      "first_name",
      "last_name",
      "address",
      "status",
      "created_by",
      "updated_by",
      "created_at",
      "updated_at"
    )(c =>
      (
        c.id.value,
        c.agentId.value,
        c.orgId.value,
        c.phoneNumber.value,
        c.firstName.value,
        c.lastName.value,
        c.address.asJson,
        c.status.entryName,
        c.createdBy,
        c.updatedBy.map(_.value),
        c.createdAt,
        c.updatedAt
      )
    )

  implicit val agentClientUpdateEncoder: Encoder[AgentClientUpdate] = Encoder.forProduct4(
    "phone_number",
    "first_name",
    "last_name",
    "address"
  )(agent =>
    (
      agent.phoneNumber.map(_.value.value),
      agent.firstName.map(_.value.value),
      agent.lastName.map(_.value.value),
      agent.address.asJson
    )
  )

  implicit val eqOrder: Eq[Order] = Eq.fromUniversalEquals

  implicit val showPaymentType: Show[PaymentType] = Show.fromToString

  implicit val orderRequestEncoder: Encoder[OrderRequest] = Encoder.forProduct4(
    "total_amount",
    "products",
    "branch_id",
    "delivery_time"
  )(r =>
    (
      r.totalAmount.value.amount,
      r.products.asJson,
      r.branchId.map(_.value),
      r.deliveryTime
    )
  )

  implicit val showOrder: Show[Order]               = Show.fromToString
  implicit val showOrderRequest: Show[OrderRequest] = Show.fromToString
  implicit val showOrderId: Show[OrderId]           = Show.fromToString

  implicit val showAgent: Show[AgentClient]                     = Show.fromToString
  implicit val showClientId: Show[ClientId]                     = Show.fromToString
  implicit val showAgentClientRequest: Show[AgentClientRequest] = Show.fromToString

  implicit val showAgentClientUpdate: Show[AgentClientUpdate] = Show.fromToString

  implicit val showStaffId: Show[StaffId]     = Show.fromToString
  implicit val showStaffAuth: Show[StaffAuth] = Show.fromToString

  implicit val showCart: Show[Cart]           = Show.fromToString
  implicit val showCartTotal: Show[CartTotal] = Show.fromToString

}
