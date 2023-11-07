package com.bbr.commerz.organization.suite

import com.bbr.commerz.organization.domain.branch.BranchPayloads._
import com.bbr.commerz.organization.domain.staff.StaffPayloads._
import com.bbr.commerz.organization.domain.organization.OrganizationPayloads._
import com.bbr.platform.domain.Branch.BranchId
import com.bbr.platform.domain.Staff._
import eu.timepit.refined.api.Refined
import org.scalacheck.Gen

trait OrgGenerators extends CommonArbitraries {

  def genStaffId: Gen[StaffId] = Gen.uuid.map(StaffId.apply)

  def genUserNameParam: Gen[UserNameParam] = genRefinedNEString.map(UserNameParam.apply)
  def genUserName: Gen[UserName]           = genString(10).map(s => UserName(s.toLowerCase.capitalize))

  def genFirstNameParam: Gen[FirstNameParam] =
    genString(10).map(s => FirstNameParam(Refined.unsafeApply(s.toLowerCase.capitalize)))
  def genFirstName: Gen[FirstName]           = genString(10).map(s => FirstName(s.toLowerCase.capitalize))

  def genLastNameParam: Gen[LastNameParam] =
    genString(10).map(s => LastNameParam(Refined.unsafeApply(s.toLowerCase.capitalize)))
  def genLastName: Gen[LastName]           = genString(10).map(s => LastName(s.toLowerCase.capitalize))

  def genPhoneNumberParam: Gen[PhoneNumberParam] =
    genPhoneNumber.map(pn => PhoneNumberParam(Refined.unsafeApply(pn.value)))

  def genStaff: Gen[Staff]        = Gen.oneOf(genAgent, genWorker)
  def genAgents: Gen[List[Staff]] = Gen.listOfN(5, genAgent)

  def genPassword: Gen[Password]                   = genShortString.map(Password.apply)
  def genPasswordParam: Gen[PasswordParam]         = genRefinedNEString.map(PasswordParam.apply)
  def genEncryptedPassword: Gen[EncryptedPassword] = genShortString.map(EncryptedPassword.apply)

  def genOrganizationRequest: Gen[OrganizationRequest] =
    genRefinedNEString.map(s => OrganizationRequest(OrganizationNameParam(s)))

  def genStaffAuth: Gen[StaffAuth] =
    for {
      id          <- genStaffId
      branchId    <- Gen.option(genBranchId)
      phoneNumber <- genPhoneNumber
      role        <- Gen.oneOf(StaffRole.values)
    } yield StaffAuth(id, branchId, phoneNumber, role)

  def genStaffAuthWithoutAgent: Gen[StaffAuth] =
    for {
      id          <- genStaffId
      branchId    <- Gen.option(genBranchId)
      phoneNumber <- genPhoneNumber
      role        <- Gen.oneOf(StaffRole.WORKER, StaffRole.OWNER)
    } yield StaffAuth(id, branchId, phoneNumber, role)

  def genEmailParam: Gen[EmailParam] = for {
    name <- genShortString
    org  <- genShortString
  } yield EmailParam(Refined.unsafeApply(s"$name@$org.com"))

  def genEmail: Gen[Email] = for {
    name <- genShortString
    org  <- genShortString
  } yield Email(s"$name@$org.com")

  def genAgent: Gen[Staff] =
    for {
      id          <- genStaffId
      orgId       <- genOrgId
      userName    <- genUserName
      password    <- genEncryptedPassword
      email       <- Gen.option(genEmail)
      phoneNumber <- genPhoneNumber
      firstName   <- Gen.option(genFirstName)
      lastName    <- Gen.option(genLastName)
      birthDate   <- Gen.option(genLocalDate)
      createdAt   <- Gen.option(genLocalDateTime)
      updatedAt   <- Gen.option(genLocalDateTime)
    } yield Staff(
      id = id,
      orgId = orgId,
      branchId = None,
      role = StaffRole.AGENT,
      userName = userName,
      password = password,
      email = email,
      phoneNumber = phoneNumber,
      firstName = firstName,
      lastName = lastName,
      birthDate = birthDate,
      status = StaffStatus.ACTIVE,
      createdAt = createdAt,
      updatedAt = updatedAt
    )

  def genWorker: Gen[Staff] =
    for {
      id          <- genStaffId
      orgId       <- genOrgId
      branchId    <- Gen.option(genBranchId)
      userName    <- genUserName
      password    <- genEncryptedPassword
      email       <- Gen.option(genEmail)
      phoneNumber <- genPhoneNumber
      firstName   <- Gen.option(genFirstName)
      lastName    <- Gen.option(genLastName)
      birthDate   <- Gen.option(genLocalDate)
      createdAt   <- Gen.option(genLocalDateTime)
      updatedAt   <- Gen.option(genLocalDateTime)
    } yield Staff(
      id = id,
      orgId = orgId,
      branchId = branchId,
      role = StaffRole.WORKER,
      userName = userName,
      password = password,
      email = email,
      phoneNumber = phoneNumber,
      firstName = firstName,
      lastName = lastName,
      birthDate = birthDate,
      status = StaffStatus.ACTIVE,
      createdAt = createdAt,
      updatedAt = updatedAt
    )

  def genStaffUpdate: Gen[StaffUpdate] =
    for {
      branchId    <- Gen.option(genBranchId)
      userName    <- Gen.option(genUserNameParam)
      email       <- Gen.option(genEmail)
      phoneNumber <- Gen.option(genPhoneNumberParam)
      firstName   <- Gen.option(genFirstName)
      lastName    <- Gen.option(genLastName)
      birthDate   <- Gen.option(genLocalDate)
    } yield StaffUpdate(
      branchId = branchId,
      userName = userName,
      email = email,
      phoneNumber = phoneNumber,
      firstName = firstName,
      lastName = lastName,
      birthDate = birthDate
    )

  def genStaffRequest: Gen[StaffRequest] =
    for {
      branchId    <- Gen.option(genBranchId)
      role        <- Gen.oneOf(StaffRole.values.filterNot(_ == StaffRole.OWNER))
      userName    <- genUserNameParam
      password    <- genPasswordParam
      email       <- Gen.option(genEmailParam)
      phoneNumber <- genPhoneNumberParam
      firstName   <- Gen.option(genFirstNameParam)
      lastName    <- Gen.option(genLastNameParam)
      birthDate   <- Gen.option(genLocalDate)
    } yield StaffRequest(
      branchId = branchId,
      role = role,
      userName = userName,
      password = password,
      email = email,
      phoneNumber = phoneNumber,
      firstName = firstName,
      lastName = lastName,
      birthDate = birthDate
    )

  def genOrganizationName: Gen[OrganizationName] = genStringNoShorterThan(10).map(OrganizationName)

  def genOrganization: Gen[Organization] =
    for {
      id        <- genOrgId
      name      <- genOrganizationName
      status    <- Gen.oneOf(OrganizationStatus.values)
      createdAt <- Gen.option(genLocalDateTime)
      updatedAt <- Gen.option(genLocalDateTime)
    } yield Organization(id, name, status, createdAt, updatedAt)

  def genBranchId: Gen[BranchId] = Gen.uuid.map(BranchId.apply)

  def genBranch: Gen[Branch] =
    for {
      id         <- genBranchId
      branchName <- genShortString.map(BranchName.apply)
      orgId      <- genOrgId
      address    <- genAddress
      status     <- Gen.oneOf(BranchStatus.values)
      createdBy  <- genStaffId
      createdAt  <- Gen.option(genLocalDateTime)
    } yield Branch(
      id = id,
      branchName = branchName,
      orgId = orgId,
      address = address,
      status = status,
      createdBy = createdBy,
      createdAt = createdAt
    )

  def genBranchRequest: Gen[BranchRequest] =
    for {
      branch  <- genRefinedNEString.map(BranchNameParam.apply)
      address <- genAddress
    } yield BranchRequest(branch, address)

  def genBranchUpdate: Gen[BranchUpdate] =
    for {
      branch  <- Gen.option(genRefinedNEString.map(BranchNameParam.apply))
      address <- Gen.option(genAddress)
    } yield BranchUpdate(branch, address)

  def genBranches: Gen[List[Branch]]            = Gen.listOfN(3, genBranch)
  def genOrganizations: Gen[List[Organization]] = Gen.listOfN(3, genOrganization)

}
