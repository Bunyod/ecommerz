package com.bbr.commerz.organization.domain.branch

import cats.implicits._
import cats.effect.IO
import com.bbr.commerz.organization.domain.staff.StaffPayloads._
import com.bbr.commerz.organization.suite.json._
import com.bbr.commerz.organization.infrastructure.postgres.{StaffRepository, OrganizationRepository}
import com.bbr.commerz.utils.ConfigOverrideChecks
import com.bbr.commerz.utils.ItUtils.{repositories, services, createOwner}
import BranchPayloads._
import com.bbr.commerz.organization.domain.owner.OwnerPayloads.Owner
import org.scalatest.EitherValues

trait BranchServiceItSpec extends ConfigOverrideChecks with EitherValues {

  private val branchService: BranchService[IO]                   = services.branch
  private val organizationRepository: OrganizationRepository[IO] = repositories.orgRepo
  private val staffRepository: StaffRepository[IO]               = repositories.staffRepo
  private val owner: Owner                                       = createOwner(genPhoneNumber.sample.get)

  fakeTest(classOf[BranchServiceItSpec])

  test("create branch [OK]") {
    forall(genOrganization) { organization =>
      for {
        createdOrg    <- organizationRepository.create(organization, owner.phoneNumber)
        staff          = genAgent.sample.get
        createdStaff  <- staffRepository.create(staff.copy(orgId = createdOrg.id, status = StaffStatus.ACTIVE))
        branchRequest  = genBranchRequest.sample.get
        createdBranch <- branchService.create(createdOrg.id, branchRequest, createdStaff.id)
        verification   =
          expect.all(
            createdBranch.branchName == branchRequest.name.toDomain,
            createdBranch.address == branchRequest.address,
            createdBranch.createdBy == createdStaff.id,
            createdBranch.status == BranchStatus.ACTIVE,
            createdBranch.orgId == createdOrg.id
          )
      } yield verification
    }
  }

  test("create branch [FAILURE]") {
    forall(genBranchRequest) { request =>
      for {
        orgId          <- organizationRepository.create(genOrganization.sample.get, owner.phoneNumber).map(_.id)
        staff           = genAgent.sample.get
        staffId        <- staffRepository.create(staff.copy(orgId = orgId, status = StaffStatus.ACTIVE)).map(_.id)
        _              <- branchService.create(orgId, request, staffId)
        createdBranch2 <- branchService.create(orgId, request, staffId).attempt
        verification    = expect.all(createdBranch2.isLeft)
      } yield verification
    }
  }

  test("create and update branch [OK]") {
    forall(genOrganization) { organization =>
      for {
        orgId         <- organizationRepository.create(organization, owner.phoneNumber).map(_.id)
        staff          = genAgent.sample.get
        createdStaff  <- staffRepository.create(staff.copy(orgId = orgId, status = StaffStatus.ACTIVE))
        branchRequest  = genBranchRequest.sample.get
        createdBranch <- branchService.create(orgId, branchRequest, createdStaff.id)
        branchUpdate   = genBranchUpdate.sample.get
        updated       <- branchService.updateById(orgId, createdBranch.id, branchUpdate)
        verification   = expect.all(
                           updated.id == createdBranch.id,
                           updated.status == BranchStatus.ACTIVE,
                           updated.orgId == orgId
                         )
      } yield verification
    }
  }

  test("create and update [FAILURE]") {
    forall(genOrganization) { organization =>
      for {
        orgId        <- organizationRepository.create(organization, owner.phoneNumber).map(_.id)
        staff         = genAgent.sample.get
        createdStaff <- staffRepository.create(staff.copy(orgId = orgId, status = StaffStatus.ACTIVE))
        branchRequest = genBranchRequest.sample.get
        _            <- branchService.create(orgId, branchRequest, createdStaff.id)
        branchUpdate  = genBranchUpdate.sample.get
        updated      <- branchService.updateById(orgId, genBranchId.sample.get, branchUpdate).attempt
        verification  =
          expect.all(
            updated.isLeft,
            updated.left.value.getMessage.contains("The branch does not exist or the new branch name is occupied")
          )
      } yield verification
    }
  }

  test("create and get branch by id [OK]") {
    forall(genOrganization) { organization =>
      for {
        orgId        <- organizationRepository.create(organization, owner.phoneNumber).map(_.id)
        staff         = genAgent.sample.get
        createdStaff <- staffRepository.create(staff.copy(orgId = orgId, status = StaffStatus.ACTIVE))
        branchRequest = genBranchRequest.sample.get
        created      <- branchService.create(organization.id, branchRequest, createdStaff.id)
        obtained     <- branchService.getById(orgId, created.id)
        verification  =
          expect.all(
            obtained.branchName == created.branchName,
            obtained.id == created.id,
            obtained.orgId == created.orgId,
            obtained.status == created.status,
            obtained.address == created.address,
            obtained.createdAt == created.createdAt,
            obtained.createdBy == created.createdBy
          )
      } yield verification
    }
  }

  test("get branch by id [FAILURE]") {
    forall(genBranchId) { branchId =>
      for {
        orgId       <- organizationRepository.create(genOrganization.sample.get, owner.phoneNumber).map(_.id)
        obtained    <- branchService.getById(orgId, branchId).attempt
        verification = expect.all(
                         obtained.isLeft,
                         obtained.left.value.getMessage.contains("Couldn't find branch with this id")
                       )
      } yield verification
    }
  }

  test("create and get branches [OK]") {
    forall(genOrganization) { organization =>
      for {
        orgId        <- organizationRepository.create(organization, owner.phoneNumber).map(_.id)
        staff         = genAgent.sample.get
        createdStaff <- staffRepository.create(staff.copy(orgId = orgId, status = StaffStatus.ACTIVE))
        branchRequest = genBranchRequest.sample.get
        created      <- branchService.create(organization.id, branchRequest, createdStaff.id)
        branches     <- branchService.getAll(orgId, created.branchName.some)
        allBranches  <- branchService.getAll(orgId, None)
        verification  =
          expect.all(branches.map(_.branchName == created.branchName).head, branches.length == 1, allBranches.nonEmpty)
      } yield verification
    }
  }

  test("create and delete branch [OK]") {
    forall(genOrganization) { organization =>
      for {
        orgId <- organizationRepository.create(organization, owner.phoneNumber).map(_.id)

        staff         = genAgent.sample.get
        createdStaff <- staffRepository.create(staff.copy(orgId = orgId, status = StaffStatus.ACTIVE))
        branchRequest = genBranchRequest.sample.get
        created      <- branchService.create(organization.id, branchRequest, createdStaff.id)
        deleted      <- branchService.deleteById(orgId, created.id)
        verification  =
          expect.all(deleted.equals("Successfully deleted"))
      } yield verification
    }
  }

  test("create and delete branch [FAILURE]") {
    forall(genOrganization) { organization =>
      for {
        orgId <- organizationRepository.create(organization, owner.phoneNumber).map(_.id)

        staff         = genAgent.sample.get
        createdStaff <- staffRepository.create(staff.copy(orgId = orgId, status = StaffStatus.ACTIVE))
        branchRequest = genBranchRequest.sample.get
        created      <- branchService.create(organization.id, branchRequest, createdStaff.id)
        _            <- branchService.deleteById(orgId, created.id)
        deleted2     <- branchService.deleteById(orgId, created.id).attempt
        verification  =
          expect.all(
            deleted2.isLeft,
            deleted2.left.value.getMessage.contains("Could not find branch with this ID")
          )

      } yield verification
    }
  }
}
