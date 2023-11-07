package com.bbr.commerz.sales.infrastructure.postgres

import cats.implicits._
import com.bbr.commerz.inventory.domain.product.ProductPayloads.Quantity
import com.bbr.commerz.organization.domain.staff.StaffPayloads._
import com.bbr.commerz.organization.http.utils.json.{addressDecoder, addressEncoder}
import com.bbr.commerz.sales.domain.agent.AgentPayloads.{AgentClient, ClientId, ClientStatus}
import com.bbr.commerz.sales.domain.order.OrderPayloads._
import com.bbr.commerz.sales.domain.transaction.TransactionPayloads._
import com.bbr.commerz.sales.http.utils.json._
import com.bbr.platform.domain.Address.Address
import com.bbr.platform.{TimeOpts, UuidOpts}
import com.bbr.platform.domain.Product.ProductId
import com.bbr.platform.domain.Staff.PhoneNumber
import com.bbr.platform.domain.Transaction.TransactionId
import doobie._
import doobie.postgres.implicits._             // DON'T REMOVE IT
import doobie.implicits.javasql._              // DON'T REMOVE IT
import doobie.postgres.circe.jsonb.implicits._ // DON'T REMOVE IT
import io.circe._
import io.circe.syntax._
import squants.market.{Money, USD}

import java.sql.Timestamp
import java.util.UUID

object Drivers {

  implicit val readClient: Read[Either[Throwable, AgentClient]] =
    Read[
      (UUID, UUID, UUID, String, String, String, Json, String, UUID, Option[UUID], Option[Timestamp], Option[Timestamp])
    ].map {
      case (
            id,
            agentId,
            orgId,
            phoneNumber,
            firstName,
            lastName,
            address,
            status,
            createdBy,
            updatedBy,
            createdAt,
            updatedAt
          ) =>
        address
          .as[Address]
          .map { address =>
            AgentClient(
              ClientId(id),
              agentId.toStaffId,
              orgId.toOrgId,
              PhoneNumber(phoneNumber),
              FirstName(firstName),
              LastName(lastName),
              address,
              ClientStatus.withName(status),
              createdBy.toStaffId,
              updatedBy.map(_.toStaffId),
              createdAt.map(_.toLocalDateTime.truncateTime),
              updatedAt.map(_.toLocalDateTime.truncateTime)
            )
          }
          .leftMap(error => new Throwable(error.getMessage()))

    }

  implicit val writeClient: Write[AgentClient] = Write[
    (
      UUID,
      UUID,
      UUID,
      String,
      String,
      String,
      Json,
      String,
      UUID,
      Option[UUID],
      Option[Timestamp],
      Option[Timestamp]
    )
  ].contramap { client =>
    (
      client.id.value,
      client.agentId.value,
      client.orgId.value,
      client.phoneNumber.value,
      client.firstName.value,
      client.lastName.value,
      client.address.asJson,
      client.status.entryName,
      client.createdBy.value,
      client.updatedBy.map(_.value),
      client.createdAt.map(Timestamp.valueOf),
      client.updatedAt.map(Timestamp.valueOf)
    )
  }

  implicit val readOrder: Read[Order] =
    Read[
      (
        UUID,
        UUID,
        UUID,
        Option[UUID],
        String,
        String,
        Json,
        BigDecimal,
        BigDecimal,
        BigDecimal,
        Option[BigDecimal],
        Option[Json],
        UUID,
        Option[UUID],
        Option[Timestamp],
        Option[Timestamp],
        Option[Timestamp]
      )
    ].map {
      case (
            id,
            staffId,
            orgId,
            branchId,
            pStatus,
            status,
            products,
            total,
            paid,
            unpaid,
            refunded,
            returned,
            createdBy,
            updatedBy,
            createdAt,
            updatedAt,
            deliveryTime
          ) =>
        Order(
          OrderId(id),
          staffId.toStaffId,
          orgId.toOrgId,
          branchId.map(_.toBranchId),
          PaymentStatus.withName(pStatus),
          OrderStatus.withName(status),
          products.as[Map[ProductId, Quantity]].toOption.getOrElse(Map.empty[ProductId, Quantity]),
          TotalAmount(Money(total, USD)),
          PaidAmount(Money(paid, USD)),
          UnpaidAmount(Money(unpaid, USD)),
          refunded.map(m => RefundedAmount(Money(m, USD))),
          returned.flatMap(_.as[Option[Map[ProductId, QuantityWithReason]]].toOption).flatten,
          createdBy.toStaffId,
          updatedBy.map(_.toStaffId),
          createdAt.map(_.toLocalDateTime.truncateTime),
          updatedAt.map(_.toLocalDateTime.truncateTime),
          deliveryTime.map(_.toLocalDateTime.truncateTime)
        )
    }

  implicit val writeOrder: Write[Order] =
    Write[
      (
        UUID,
        UUID,
        UUID,
        Option[UUID],
        String,
        String,
        Json,
        BigDecimal,
        BigDecimal,
        BigDecimal,
        Option[BigDecimal],
        Option[Json],
        UUID,
        Option[UUID],
        Option[Timestamp],
        Option[Timestamp],
        Option[Timestamp]
      )
    ].contramap { order =>
      (
        order.id.value,
        order.staffId.value,
        order.orgId.value,
        order.branchId.map(_.value),
        order.paymentStatus.entryName,
        order.orderStatus.entryName,
        order.products.asJson,
        order.totalAmount.value.amount,
        order.paidAmount.value.amount,
        order.unpaidAmount.value.amount,
        order.refundedAmount.map(_.value.amount),
        order.returnedItems.map(_.asJson),
        order.createdBy.value,
        order.updatedBy.map(_.value),
        order.createdAt.map(Timestamp.valueOf),
        order.updatedAt.map(Timestamp.valueOf),
        order.deliveryTime.map(Timestamp.valueOf)
      )
    }

  implicit val readTransaction: Read[Transaction] =
    Read[
      (
        UUID,
        UUID,
        UUID,
        UUID,
        Option[UUID],
        Json,
        String,
        BigDecimal,
        Option[Int],
        Option[Timestamp],
        Option[Timestamp],
        Option[Timestamp]
      )
    ].map {
      case (
            id,
            orgId,
            staffId,
            orderId,
            branchId,
            products,
            paymentType,
            amount,
            discount,
            deadline,
            createdAt,
            updatedAt
          ) =>
        Transaction(
          id = TransactionId(id),
          orgId = orgId.toOrgId,
          staffId = staffId.toStaffId,
          orderId = OrderId(orderId),
          branchId = branchId.map(_.toBranchId),
          products = products.as[Map[ProductId, Quantity]].toOption.getOrElse(Map.empty[ProductId, Quantity]),
          paymentType = PaymentType.withName(paymentType),
          amount = TotalAmount(Money(amount, USD)),
          discount = discount.map(Discount.apply),
          deadline = deadline.map(_.toLocalDateTime.truncateTime),
          createdAt = createdAt.map(_.toLocalDateTime.truncateTime),
          updatedAt = updatedAt.map(_.toLocalDateTime.truncateTime)
        )
    }

  implicit val writeTransaction: Write[Transaction] =
    Write[
      (
        UUID,
        UUID,
        UUID,
        UUID,
        Option[UUID],
        Json,
        String,
        BigDecimal,
        Option[Int],
        Option[Timestamp],
        Option[Timestamp],
        Option[Timestamp]
      )
    ].contramap { transaction =>
      (
        transaction.id.value,
        transaction.orgId.value,
        transaction.staffId.value,
        transaction.orderId.value,
        transaction.branchId.map(_.value),
        transaction.products.asJson,
        transaction.paymentType.entryName,
        transaction.amount.value.amount,
        transaction.discount.map(_.value),
        transaction.deadline.map(Timestamp.valueOf),
        transaction.createdAt.map(Timestamp.valueOf),
        transaction.updatedAt.map(Timestamp.valueOf)
      )
    }

}
