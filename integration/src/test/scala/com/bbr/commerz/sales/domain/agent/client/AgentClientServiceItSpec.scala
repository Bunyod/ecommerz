package com.bbr.commerz.sales.domain.agent.client

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.bbr.commerz.organization.domain.organization.OrganizationService
import com.bbr.commerz.organization.domain.owner.OwnerPayloads.Owner
import com.bbr.commerz.organization.domain.staff.StaffService
import com.bbr.commerz.sales.suite.json._
import com.bbr.commerz.utils.ConfigOverrideChecks
import com.bbr.commerz.utils.ItUtils._
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.{StaffId, StaffRole}

trait AgentClientServiceItSpec extends ConfigOverrideChecks {

  private val service = services.agent

  private val orgService: OrganizationService[IO] = services.organization
  private val staffService: StaffService[IO]      = services.staff
  private val owner: Owner                        = createOwner(genPhoneNumber.sample.get)
  private val orgId: OrgId                        =
    orgService.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id).unsafeRunSync()
  private val staffId: StaffId                    = staffService
    .create(orgId, genStaffRequest.sample.get.copy(branchId = None, role = StaffRole.AGENT))
    .map(_.id)
    .unsafeRunSync()

  fakeTest(classOf[AgentClientServiceItSpec])

  test("create agent client [OK]") {
    forall(genAgentClientRequest) { request =>
      for {
        created     <- service.create(orgId, staffId, request)
        verification =
          expect.all(created.address == request.address, created.phoneNumber.value == request.phoneNumber.value.value)
      } yield verification
    }
  }
  test("create agent client, non-existing client in this organization [FAILURE]") {
    forall(genAgentClientRequest) { request =>
      for {
        _           <- service.create(orgId, staffId, request)
        created2    <- service.create(orgId, staffId, request).attempt
        verification =
          expect.all(created2.isLeft)
      } yield verification
    }
  }

  test("update agent client [OK]") {
    forall(genAgentClientUpdate) { update =>
      val request = genAgentClientRequest.sample.get
      for {
        clientId    <- service.create(orgId, staffId, request).map(_.id)
        obtained    <- service.getById(orgId, staffId, clientId)
        updated     <- service.updateById(orgId, staffId, clientId, update)
        verification = expect.all(updated.id == clientId, updated.createdBy == obtained.createdBy)
      } yield verification
    }
  }
  test("update agent client [FAILURE]") {
    forall(genAgentClientUpdate) { update =>
      val clientId = genAgentClientId.sample.get
      for {
        updated     <- service.updateById(orgId, staffId, clientId, update).attempt
        verification = expect.all(updated.isLeft)
      } yield verification
    }
  }

  test("create agent client and get by ID [OK]") {
    forall(genAgentClientRequest) { request =>
      for {
        id          <- service.create(orgId, staffId, request).map(_.id)
        obtained    <- service.getById(orgId, staffId, id)
        verification = expect.all(obtained.orgId == orgId)
      } yield verification
    }
  }
  test("create agent client and get by ID [FAILURE]") {
    forall(genAgentClientId) { id =>
      for {
        obtained    <- service.getById(orgId, staffId, id).attempt
        verification = expect.all(obtained.isLeft)
      } yield verification
    }
  }

  test("create agent client and get all clients [OK]") {
    forall(genAgentClientRequest) { request =>
      for {
        created     <- service.create(orgId, staffId, request)
        obtained    <- service.getAll(orgId, staffId, Some(50), Some(0))
        verification = expect.all(obtained.map(_.phoneNumber).contains(created.phoneNumber))
      } yield verification
    }
  }

  test("create and delete agent client [OK]") {
    forall(genAgentClientRequest) { request =>
      for {
        clientId    <- service.create(orgId, staffId, request).map(_.id)
        obtained    <- service.getById(orgId, staffId, clientId)
        _           <- service.deleteById(orgId, staffId, clientId)
        verification = expect.all(obtained.orgId == orgId)
      } yield verification
    }
  }
  test("create and delete agent client [FAILURE]") {
    forall(genAgentClientRequest) { request =>
      for {
        _           <- service.create(orgId, staffId, request)
        deleted     <- service.deleteById(orgId, staffId, genAgentClientId.sample.get).attempt
        verification = expect.all(deleted.isRight)
      } yield verification
    }
  }

}
