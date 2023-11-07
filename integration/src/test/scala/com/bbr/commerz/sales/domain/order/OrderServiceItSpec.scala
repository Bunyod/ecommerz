package com.bbr.commerz.sales.domain.order

import cats.implicits._
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.bbr.commerz.organization.domain.owner.OwnerPayloads.Owner
import com.bbr.commerz.sales.domain.order.OrderPayloads.{OrderStatus, QuantityWithReason, ReturnReason}
import com.bbr.commerz.sales.domain.transaction.TransactionPayloads.RefundedAmount
import com.bbr.commerz.sales.suite.json._
import com.bbr.commerz.utils.ConfigOverrideChecks
import com.bbr.commerz.utils.ItUtils.{createOwner, services}
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.StaffId

trait OrderServiceItSpec extends ConfigOverrideChecks {

  private val service: OrderService[IO] = services.order

  private val owner: Owner     = createOwner(genPhoneNumber.sample.get)
  private val orgId: OrgId     =
    services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id).unsafeRunSync()
  private val staffId: StaffId =
    services.staff.create(orgId, genStaffRequest.sample.get.copy(branchId = None)).map(_.id).unsafeRunSync()

  fakeTest(classOf[OrderServiceItSpec])

  test("create and get order [OK]") {
    forall(genOrderRequest) { request =>
      for {
        created  <- service.create(orgId, staffId, request.copy(branchId = None))
        obtained <- service.getById(created.orgId, created.id)
      } yield expect.eql(created, obtained)
    }
  }

  test("get all orders [OK]") {
    forall(genOrderRequest) { request =>
      for {
        orgId    <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        _        <- service.create(orgId, staffId, request.copy(branchId = None))
        _        <- service.create(orgId, staffId, genOrderRequest.sample.get.copy(branchId = None))
        obtained <- service.getAll(orgId, None, None)
      } yield expect.all(obtained.nonEmpty, obtained.size == 2)
    }
  }

  test("update order status [OK]") {
    forall(genOrderRequest) { request =>
      for {
        created <- service.create(orgId, staffId, request.copy(branchId = None))
        _       <- service.updateStatus(orgId, created.id, OrderStatus.CONFIRMED)
        updated <- service.getById(orgId, created.id)
      } yield expect.all(
        created.orderStatus == OrderStatus.PENDING,
        updated.orderStatus == OrderStatus.CONFIRMED
      )
    }
  }

  test("return order [OK]") {
    forall(genOrderRequest) { request =>
      for {
        created      <- service.create(orgId, staffId, request.copy(branchId = None))
        updateRequest = genOrderReturn.sample.get.copy(
                          returnedItems = created.products.map { case (productId, quantity) =>
                            productId -> QuantityWithReason(quantity, ReturnReason.EXPIRED)
                          }.some,
                          refundedAmount = RefundedAmount(created.paidAmount.value).some
                        )
        _            <- service.setReturnedProducts(orgId, created.id, updateRequest)
        updated      <- service.getById(orgId, created.id)
      } yield expect.all(
        updateRequest.deliveryTime == updated.deliveryTime,
        updateRequest.returnedItems == updated.returnedItems,
        updateRequest.refundedAmount == updated.refundedAmount
      )
    }
  }

  test("update order [OK]") {
    forall(genOrderRequest) { request =>
      for {
        created      <- service.create(orgId, staffId, request.copy(branchId = None))
        updateRequest = genOrderUpdate.sample.get
        updated      <- service.updateById(orgId, created.id, staffId, updateRequest)
        obtained     <- service.getById(orgId, created.id)
      } yield expect.all(
        updated.updatedBy == obtained.updatedBy,
        updated.paidAmount == obtained.paidAmount,
        updated.updatedAt == obtained.updatedAt,
        updated.refundedAmount == obtained.refundedAmount
      )
    }
  }
}
