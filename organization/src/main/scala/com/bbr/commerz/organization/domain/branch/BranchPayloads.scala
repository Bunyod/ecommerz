package com.bbr.commerz.organization.domain.branch

import com.bbr.platform.domain.Address.Address
import com.bbr.platform.domain.Branch.BranchId
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.StaffId
import enumeratum._
import eu.timepit.refined.types.string.NonEmptyString

import java.time.LocalDateTime

object BranchPayloads {

  sealed trait BranchStatus extends EnumEntry
  object BranchStatus       extends Enum[BranchStatus] with CirceEnum[BranchStatus] {
    val values: IndexedSeq[BranchStatus] = findValues
    case object ACTIVE   extends BranchStatus
    case object INACTIVE extends BranchStatus
  }

  case class BranchName(value: String)
  case class BranchNameParam(value: NonEmptyString) {
    def toDomain: BranchName = BranchName(value.value)
  }
  case class BranchRequest(
    name: BranchNameParam,
    address: Address
  ) {
    def toDomain(
      id: BranchId,
      orgId: OrgId,
      createdBy: StaffId,
      createdAt: Option[LocalDateTime]
    ): Branch =
      Branch(
        id = id,
        orgId = orgId,
        branchName = name.toDomain,
        status = BranchStatus.ACTIVE,
        createdBy = createdBy,
        address = address,
        createdAt = createdAt
      )
  }

  case class BranchUpdate(
    name: Option[BranchNameParam],
    address: Option[Address]
  )

  case class Branch(
    id: BranchId,
    orgId: OrgId,
    branchName: BranchName,
    address: Address,
    status: BranchStatus,
    createdBy: StaffId,
    createdAt: Option[LocalDateTime]
  )
}
