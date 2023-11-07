package com.bbr.commerz.sales.domain.transaction

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.bbr.commerz.organization.domain.organization.OrganizationService
import com.bbr.commerz.organization.domain.owner.OwnerPayloads.Owner
import com.bbr.commerz.organization.domain.staff.StaffService
import com.bbr.commerz.sales.domain.order.OrderPayloads.OrderId
import com.bbr.commerz.sales.http.utils.json._
import com.bbr.commerz.utils.ConfigOverrideChecks
import com.bbr.commerz.utils.ItUtils._
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.{StaffId, StaffRole}

trait TransactionServiceItSpec extends ConfigOverrideChecks {

  private val service: TransactionService[IO]     = services.transaction
  private val orgService: OrganizationService[IO] = services.organization
  private val staffService: StaffService[IO]      = services.staff
  private val owner: Owner                        = createOwner(genPhoneNumber.sample.get)

  private val orgId: OrgId     =
    orgService.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id).unsafeRunSync()
  private val staffId: StaffId = staffService
    .create(orgId, genStaffRequest.sample.get.copy(branchId = None, role = StaffRole.AGENT))
    .map(_.id)
    .unsafeRunSync()
  private val orderId: OrderId =
    services.order
      .create(orgId, staffId, genOrderRequest.sample.get.copy(branchId = None))
      .map(_.id)
      .unsafeRunSync()

  fakeTest(classOf[TransactionServiceItSpec])

  test("create transaction [OK]") {
    forall(genTransactionRequest) { request =>
      for {
        created     <- service.create(orgId, staffId, request.copy(branchId = None, orderId = orderId))
        verification = expect.all(created.amount == request.totalAmount, created.orderId == orderId)
      } yield verification
    }
  }

  test("create transaction, non-existing organization [FAILURE]") {
    forall(genTransactionRequest) { request =>
      for {
        created     <-
          service.create(genOrgId.sample.get, staffId, request.copy(branchId = None, orderId = orderId)).attempt
        verification = expect.all(created.isLeft)
      } yield verification
    }
  }

  test("create and get transaction by ID [OK]") {
    forall(genTransactionRequest) { request =>
      for {
        transactionId <- service.create(orgId, staffId, request.copy(branchId = None, orderId = orderId)).map(_.id)
        obtained      <- service.getById(orgId, transactionId)
        verification   = expect.all(obtained.orgId == orgId)
      } yield verification
    }
  }
  test("create and get transaction, non-existing transaction [FAILURE]") {
    forall(genTransactionId) { id =>
      for {
        obtained    <- service.getById(orgId, id).attempt
        verification = expect.all(obtained.isLeft)
      } yield verification
    }
  }

  test("create and get transactions [OK]") {
    forall(genTransactionRequest) { request =>
      for {
        orgId       <- orgService.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        staffId     <-
          staffService.create(orgId, genStaffRequest.sample.get.copy(branchId = None, role = StaffRole.AGENT)).map(_.id)
        orderId     <- services.order.create(orgId, staffId, genOrderRequest.sample.get.copy(branchId = None)).map(_.id)
        _           <- service.create(orgId, staffId, request.copy(branchId = None, orderId = orderId))
        _           <-
          service.create(orgId, staffId, genTransactionRequest.sample.get.copy(branchId = None, orderId = orderId))
        obtained    <- service.getAll(orgId)
        verification = expect.all(obtained.length == 2)
      } yield verification
    }
  }

  test("create and delete transaction [OK]") {
    forall(genTransactionRequest) { request =>
      for {
        transactionId <- service.create(orgId, staffId, request.copy(branchId = None, orderId = orderId)).map(_.id)
        obtained      <- service.getById(orgId, transactionId)
        deleted       <- service.deleteById(orgId, transactionId)
        verification   = expect.all(obtained.orgId == orgId, deleted.equals("Successfully deleted."))
      } yield verification
    }
  }
  test("create and delete transaction [FAILURE]") {
    forall(genTransactionRequest) { request =>
      for {
        _           <- service.create(orgId, staffId, request.copy(branchId = None, orderId = orderId)).map(_.id)
        deleted     <- service.deleteById(orgId, genTransactionId.sample.get).attempt
        verification = expect.all(deleted.isLeft)
      } yield verification
    }
  }
}
