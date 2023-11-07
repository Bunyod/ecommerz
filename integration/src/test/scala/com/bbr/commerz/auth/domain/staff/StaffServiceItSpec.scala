package com.bbr.commerz.auth.domain.staff

import cats.implicits._
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.bbr.commerz.organization.domain.staff.StaffPayloads.StaffStatus
import com.bbr.commerz.organization.infrastructure.postgres.{OrganizationRepository, StaffRepository}
import com.bbr.commerz.sales.suite.json.showStaffId
import com.bbr.commerz.utils.ItUtils._
import com.bbr.commerz.utils.ConfigOverrideChecks
import com.bbr.commerz.organization.domain.organization.OrganizationPayloads.Organization
import com.bbr.commerz.organization.domain.owner.OwnerPayloads.Owner
import com.bbr.commerz.organization.domain.staff.StaffService
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.StaffRole

trait StaffServiceItSpec extends ConfigOverrideChecks {

  private val staffRepository: StaffRepository[IO]      = repositories.staffRepo
  private val staffService: StaffService[IO]            = services.staff
  private val orgRepository: OrganizationRepository[IO] = repositories.orgRepo
  private val organization: Organization                = genOrganization.sample.get

  private val owner: Owner = createOwner(genPhoneNumber.sample.get)
  private val orgId: OrgId = orgRepository.create(organization, owner.phoneNumber).unsafeRunSync().id

  fakeTest(classOf[StaffServiceItSpec])

  test("create and get workers [OK]") {
    forall(genWorker) { worker =>
      for {
        staffId      <- repositories.staffRepo.create(genAgent.sample.get.copy(orgId = orgId)).map(_.id)
        branchId     <- services.branch.create(orgId, genBranchRequest.sample.get, staffId).map(_.id)
        created      <-
          staffRepository.create(worker.copy(orgId = orgId, branchId = branchId.some, status = StaffStatus.ACTIVE))
        allWorkers   <- staffService.getWorkers(orgId)
        worker       <- staffService.getWorkers(orgId, created.userName.value.some)
        verifications = expect.all(
                          allWorkers.nonEmpty,
                          worker.length == 1
                        )
      } yield verifications
    }
  }

  test("create and get agents [OK]") {
    forall(genStaffRequest) { request =>
      for {
        created      <- staffService.create(orgId, request.copy(role = StaffRole.AGENT, branchId = None))
        allAgents    <- staffService.getAgents(orgId)
        agent        <- staffService.getAgents(orgId, created.userName.value.some)
        verifications = expect.all(
                          allAgents.nonEmpty,
                          agent.length == 1
                        )
      } yield verifications
    }
  }

  test("create and get staff by ID [OK]") {
    forall(genStaffRequest) { request =>
      for {
        staffId     <- services.staff.create(orgId, genStaffRequest.sample.get.copy(branchId = None)).map(_.id)
        branchId    <- services.branch.create(orgId, genBranchRequest.sample.get, staffId).map(_.id)
        created     <- services.staff.create(orgId, request.copy(branchId = branchId.some))
        obtained    <- staffService.getById(orgId, created.id)
        verification = expect.all(
                         obtained.id == created.id,
                         obtained.userName == created.userName,
                         obtained.orgId == orgId,
                         obtained.status == created.status,
                         obtained.birthDate == created.birthDate,
                         obtained.lastName == created.lastName,
                         obtained.firstName == created.firstName,
                         obtained.createdAt == created.createdAt
                       )
      } yield verification
    }
  }

  test("create and get staff by ID [FAILURE]") {
    forall(genStaffId) { staffId =>
      for {
        obtained    <- staffService.getById(orgId, staffId).attempt
        verification = expect.all(obtained.isLeft)
      } yield verification
    }
  }

  test("create and update staff by ID [OK]") {
    forall(genAgent) { staff =>
      for {
        created      <- staffRepository.create(staff.copy(orgId = orgId))
        updateRequest = genStaffUpdate.sample.get.copy(branchId = None)
        updated      <- staffService.updateById(orgId, created.id, updateRequest)
        verifications = expect.all(
                          updated.id == created.id,
                          updated.createdAt == created.createdAt,
                          updated.orgId == created.orgId
                        )
      } yield verifications
    }
  }

  test("update staff by ID [FAILURE]") {
    forall(genStaffId) { staffId =>
      for {
        updated      <- staffService.updateById(orgId, staffId, genStaffUpdate.sample.get).attempt
        verifications = expect.all(updated.isLeft)
      } yield verifications
    }
  }

  test("create and delete staff [OK]") {
    forall(genAgent) { staff =>
      for {
        created      <- staffRepository.create(staff.copy(orgId = orgId, status = StaffStatus.ACTIVE))
        deleted      <- staffService.deleteById(orgId, created.id)
        verifications = expect.all(deleted.equals("Successfully deleted"))
      } yield verifications
    }
  }

  test("create and delete staff [FAILURE]") {
    forall(genAgent) { staff =>
      for {
        created      <- staffRepository.create(staff.copy(orgId = orgId, status = StaffStatus.ACTIVE))
        _            <- staffService.deleteById(orgId, created.id)
        deleted      <- staffService.deleteById(orgId, created.id).attempt
        verifications = expect.all(deleted.isLeft)
      } yield verifications
    }
  }
}
