package com.bbr.commerz.sales.http.utils

import cats.Show
import com.bbr.commerz.inventory.domain.product.ProductPayloads._
import com.bbr.commerz.inventory.http.utils.json._
import com.bbr.commerz.organization.domain.staff.StaffPayloads._
import com.bbr.commerz.organization.http.utils.json._
import com.bbr.commerz.organization.http.utils.refined.{Name, PhoneNumberPred}
import com.bbr.commerz.sales.domain.agent.AgentPayloads._
import com.bbr.commerz.sales.domain.cart.CartPayloads.{Cart, CartTotal}
import com.bbr.commerz.sales.domain.order.OrderPayloads._
import com.bbr.commerz.sales.domain.transaction.TransactionPayloads._
import com.bbr.platform.domain.Address.Address
import com.bbr.platform.domain.Branch.BranchId
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Product.ProductId
import com.bbr.platform.domain.Staff.{PhoneNumber, PhoneNumberParam, StaffId}
import com.bbr.platform.domain.Transaction.TransactionId
import eu.timepit.refined.auto._
import eu.timepit.refined.types.all.PosInt
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.generic.semiauto._
import io.circe.refined._
import io.circe.syntax.EncoderOps
import org.http4s.EntityEncoder
import org.http4s.circe._
import squants.market.{Money, USD}

import java.time.LocalDateTime
import java.util.UUID

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
  implicit val paymentTypeDecoder: Decoder[PaymentType] = deriveConfiguredDecoder[PaymentType]

  implicit val orderStatusEncoder: Encoder[OrderStatus] =
    Encoder.forProduct1("order_status")(_.entryName)
  implicit val orderStatusDecoder: Decoder[OrderStatus] =
    Decoder.forProduct1("order_status")(s => OrderStatus.withName(s))

  implicit val totalAmountEncoder: Encoder[TotalAmount] = deriveEncoder[TotalAmount]
  implicit val totalAmountDecoder: Decoder[TotalAmount] = deriveDecoder[TotalAmount]

  implicit val payedAmountEncoder: Encoder[PaidAmount] = deriveEncoder[PaidAmount]
  implicit val payedAmountDecoder: Decoder[PaidAmount] = deriveDecoder[PaidAmount]

  implicit val discountEncoder: Encoder[Discount] = deriveEncoder[Discount]
  implicit val discountDecoder: Decoder[Discount] = deriveDecoder[Discount]

  implicit val transactionIdEncoder: Encoder[TransactionId] = deriveEncoder[TransactionId]
  implicit val transactionIdDecoder: Decoder[TransactionId] = deriveDecoder[TransactionId]

  implicit val clientIdEncoder: Encoder[ClientId] =
    Encoder.forProduct1("client_id")(_.value)

  implicit val clientIdDecoder: Decoder[ClientId] =
    Decoder.forProduct1("client_id")(ClientId.apply)

  implicit val transactionRequestEncoder: Encoder[TransactionRequest] = Encoder.forProduct6(
    "order_id",
    "branch_id",
    "deadline",
    "payment_type",
    "total_amount",
    "discount"
  )(tr =>
    (
      tr.orderId.value,
      tr.branchId.map(_.value),
      tr.deadline,
      tr.paymentType.entryName,
      tr.totalAmount.value.amount,
      tr.discount.map(_.value.value)
    )
  )

  implicit val transactionRequestDecoder: Decoder[TransactionRequest] = Decoder.instance { h =>
    for {
      orderId     <- h.get[UUID]("order_id").map(OrderId.apply)
      branchId    <- h.downField("branch_id").as[Option[UUID]].map(_.map(BranchId.apply))
      deadline    <- h.downField("deadline").as[Option[LocalDateTime]]
      paymentType <- h.get[String]("payment_type").map(p => PaymentType.withName(p))
      totalAmount <- h.get[BigDecimal]("total_amount").map(t => TotalAmount(Money(t, USD)))
      discount    <- h.downField("discount").as[Option[PosInt]].map(_.map(DiscountParam.apply))
    } yield TransactionRequest(
      orderId = orderId,
      branchId = branchId,
      deadline = deadline,
      paymentType = paymentType,
      totalAmount = totalAmount,
      discount = discount
    )
  }

  implicit val transactionEncoder: Encoder[Transaction] = Encoder.forProduct12(
    "transaction_id",
    "org_id",
    "staff_id",
    "order_id",
    "branch_id",
    "products",
    "payment_type",
    "amount",
    "discount",
    "deadline",
    "created_at",
    "updated_at"
  )(tr =>
    (
      tr.id.value,
      tr.orgId.value,
      tr.staffId.value,
      tr.orderId.value,
      tr.branchId.map(_.value),
      tr.products.map(v => (v._1.value, v._2.value)),
      tr.paymentType.entryName,
      tr.amount.value.amount,
      tr.discount.map(_.value),
      tr.deadline,
      tr.createdAt,
      tr.updatedAt
    )
  )

  implicit val transactionDecoder: Decoder[Transaction] = Decoder.instance { h =>
    for {
      transactionId <- h.get[UUID]("transaction_id").map(TransactionId.apply)
      orgId         <- h.get[UUID]("org_id").map(OrgId.apply)
      staffId       <- h.get[UUID]("staff_id").map(StaffId.apply)
      orderId       <- h.get[UUID]("order_id").map(OrderId.apply)
      branchId      <- h.downField("branch_id").as[Option[UUID]].map(_.map(BranchId.apply))
      products      <- h.get[Map[UUID, Int]]("products").map(_.map(v => (ProductId(v._1), Quantity(v._2))))
      paymentType   <- h.get[String]("payment_type").map(p => PaymentType.withName(p))
      amount        <- h.get[BigDecimal]("amount").map(t => TotalAmount(Money(t, USD)))
      discount      <- h.downField("discount").as[Option[Int]].map(_.map(Discount.apply))
      deadline      <- h.downField("deadline").as[Option[LocalDateTime]]
      createdAt     <- h.downField("created_at").as[Option[LocalDateTime]]
      updatedAt     <- h.downField("updated_at").as[Option[LocalDateTime]]
    } yield Transaction(
      id = transactionId,
      orgId = orgId,
      staffId = staffId,
      orderId = orderId,
      branchId = branchId,
      products = products,
      paymentType = paymentType,
      amount = amount,
      discount = discount,
      deadline = deadline,
      createdAt = createdAt,
      updatedAt = updatedAt
    )
  }
  implicit val showTransaction: Show[Transaction]       = Show.fromToString

  implicit val showTransactionRequest: Show[TransactionRequest] = Show.fromToString

  implicit val cartTotalEncoder: Encoder[CartTotal] = deriveEncoder[CartTotal]

  implicit val cartProductEncoder: Encoder[CartProduct] = deriveEncoder[CartProduct]

  implicit val mapDecoder: Decoder[Map[ProductId, Quantity]] =
    Decoder.decodeMap[String, Int].map { decoded =>
      decoded.map { case (key, value) =>
        ProductId(UUID.fromString(key)) -> Quantity(value)
      }
    }

  implicit val mapEncoder: Encoder[Map[ProductId, Quantity]] =
    Encoder.instance { map =>
      Json.fromJsonObject(JsonObject.fromIterable(map.map { case (key, value) =>
        key.value.toString -> value.value.asJson
      }))
    }

  implicit val withReasonEncoder: Encoder[QuantityWithReason] =
    Encoder.forProduct2("quantity", "reason")(r => (r.quantity.value, r.reason.entryName))
  implicit val withReasonDecoder: Decoder[QuantityWithReason] =
    Decoder.instance { h =>
      for {
        quantity <- h.get[Int]("quantity").map(Quantity.apply)
        reason   <- h.get[String]("reason").map(r => ReturnReason.withName(r))
      } yield QuantityWithReason(quantity, reason)
    }

  implicit val mapReasonDecoder: Decoder[Map[ProductId, QuantityWithReason]] =
    Decoder.decodeMap[String, QuantityWithReason].map { decoded =>
      decoded.map { case (key, value) =>
        ProductId(UUID.fromString(key)) -> value
      }
    }

  implicit val mapReasonEncoder: Encoder[Map[ProductId, QuantityWithReason]] =
    Encoder.instance { map =>
      Json.fromJsonObject(JsonObject.fromIterable(map.map { case (key, value) =>
        key.value.toString -> withReasonEncoder(value)
      }))
    }

  implicit val transactionIdShow: Show[TransactionId] = Show.fromToString

  implicit val orderIdShow: Show[OrderId] = Show.fromToString

  implicit val orderIdDecoder: Decoder[OrderId] = deriveDecoder[OrderId]
  implicit val orderIdEncoder: Encoder[OrderId] = deriveEncoder[OrderId]

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

  implicit val orderUpdateDecoder: Decoder[OrderUpdate] = Decoder.instance { h =>
    for {
      ps <- h.downField("products").as[Option[Map[ProductId, Quantity]]]
      pa <- h.downField("paid_amount").as[Option[Money]].map(_.map(PaidAmount.apply))
      ra <- h.downField("refunded_amount").as[Option[Money]].map(_.map(RefundedAmount.apply))
      dt <- h.downField("delivery_time").as[Option[LocalDateTime]]
    } yield OrderUpdate(
      products = ps,
      paidAmount = pa,
      refundedAmount = ra,
      deliveryTime = dt
    )
  }

  implicit val orderRequestEncoder: Encoder[OrderRequest] = Encoder.forProduct4(
    "total_amount",
    "products",
    "branch_id",
    "delivery_time"
  )(o =>
    (
      o.totalAmount.value.amount,
      o.products.map(v => (v._1.value, v._2.value)),
      o.branchId.map(_.value),
      o.deliveryTime
    )
  )

  implicit val orderRequestDecoder: Decoder[OrderRequest] = Decoder.instance { h =>
    for {
      total        <- h.get[BigDecimal]("total_amount").map(m => TotalAmount(Money(m, USD)))
      products     <- h.get[Map[UUID, Int]]("products").map(_.map(v => (ProductId(v._1), Quantity(v._2))))
      bid          <- h.downField("branch_id").as[Option[UUID]].map(_.map(BranchId.apply))
      deliveryTime <- h.downField("delivery_time").as[Option[LocalDateTime]]
    } yield OrderRequest(total, products, bid, deliveryTime)
  }

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

  implicit val refundedAmount: Decoder[RefundedAmount] =
    Decoder.instance(h => h.get[BigDecimal]("refunded_amount").map(m => RefundedAmount(Money(m, USD))))

  implicit val orderReturnDecoder: Decoder[OrderReturn] = Decoder.instance { h =>
    for {
      amount <- h.downField("refunded_amount").as[Option[Money]].map(_.map(RefundedAmount.apply))
      items  <- h.downField("returned_items").as[Option[Map[ProductId, QuantityWithReason]]]
      time   <- h.downField("delivery_time").as[Option[LocalDateTime]]
    } yield OrderReturn(amount, items, time)
  }

  implicit val orderDecoder: Decoder[Order] = Decoder.instance { h =>
    for {
      oid   <- h.get[UUID]("id").map(OrderId.apply)
      sid   <- h.get[UUID]("staff_id").map(StaffId.apply)
      orgId <- h.get[UUID]("org_id").map(OrgId.apply)
      bid   <- h.downField("branch_id").as[Option[UUID]].map(_.map(BranchId.apply))
      ps    <- h.get[String]("payment_status").map(s => PaymentStatus.withName(s))
      st    <- h.get[String]("order_status").map(s => OrderStatus.withName(s))
      p     <- h.get[Map[UUID, Int]]("products").map(_.map(v => (ProductId(v._1), Quantity(v._2))))
      total <- h.get[BigDecimal]("total_amount").map(m => TotalAmount(Money(m, USD)))
      pa    <- h.get[BigDecimal]("paid_amount").map(m => PaidAmount(Money(m, USD)))
      ua    <- h.get[BigDecimal]("unpaid_amount").map(m => UnpaidAmount(Money(m, USD)))
      ra    <- h.downField("refunded_amount").as[Option[Money]].map(_.map(RefundedAmount.apply))
      ri    <- h.downField("returned_items").as[Option[Map[ProductId, QuantityWithReason]]]
      cb    <- h.get[UUID]("created_by").map(StaffId.apply)
      ub    <- h.downField("updated_by").as[Option[UUID]].map(_.map(StaffId.apply))
      cat   <- h.downField("created_at").as[Option[LocalDateTime]]
      uat   <- h.downField("updated_at").as[Option[LocalDateTime]]
      dt    <- h.downField("delivery_time").as[Option[LocalDateTime]]
    } yield Order(oid, sid, orgId, bid, ps, st, p, total, pa, ua, ra, ri, cb, ub, cat, uat, dt)
  }
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

  implicit val orderShow: Show[Order] = Show.fromToString

  implicit val paymentFormDecoder: Decoder[PaymentTypeParam] =
    Decoder.forProduct1("payment_type")(PaymentTypeParam.apply)

  implicit val cartEncoder: Encoder[Cart] = deriveEncoder[Cart]
  implicit val cartDecoder: Decoder[Cart] =
    Decoder.instance(h => h.get[Map[ProductId, Quantity]]("products").map(Cart.apply))

  implicit val agentClientRequestDecoder: Decoder[AgentClientRequest] =
    Decoder.instance { h =>
      for {
        pn <- h.get[PhoneNumberPred]("phone_number").map(PhoneNumberParam(_))
        fn <- h.get[Name]("first_name").map(FirstNameParam)
        ln <- h.get[Name]("last_name").map(LastNameParam)
        ad <- h.get[Address]("address")
      } yield AgentClientRequest(pn, fn, ln, ad)
    }

  implicit val agentClientUpdateDecoder: Decoder[AgentClientUpdate] =
    Decoder.instance { h =>
      for {
        pn <- h.downField("phone_number").as[Option[PhoneNumberPred]].map(_.map(s => PhoneNumberParam(s)))
        fn <- h.downField("first_name").as[Option[Name]].map(_.map(f => FirstNameParam(f)))
        ln <- h.downField("last_name").as[Option[Name]].map(_.map(l => LastNameParam(l)))
        ad <- h.downField("address").as[Option[Address]]
      } yield AgentClientUpdate(pn, fn, ln, ad)
    }

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

  implicit val agentClientDecoder: Decoder[AgentClient] = Decoder.instance { h =>
    for {
      id <- h.get[UUID]("client_id").map(ClientId.apply)
      ag <- h.get[UUID]("agent_id").map(StaffId.apply)
      or <- h.get[UUID]("org_id").map(OrgId.apply)
      ph <- h.get[PhoneNumberPred]("phone_number").map(x => PhoneNumber(x))
      fn <- h.get[String]("first_name").map(FirstName.apply)
      ln <- h.get[String]("last_name").map(LastName.apply)
      ad <- h.get[Address]("address")
      st <- h.get[String]("status").map(ClientStatus.withName)
      cr <- h.get[UUID]("created_by").map(StaffId.apply)
      up <- h.downField("updated_by").as[Option[UUID]].map(_.map(StaffId.apply))
      ca <- h.downField("created_at").as[Option[LocalDateTime]]
      ua <- h.downField("updated_at").as[Option[LocalDateTime]]
    } yield AgentClient(id, ag, or, ph, fn, ln, ad, st, cr, up, ca, ua)
  }
}
