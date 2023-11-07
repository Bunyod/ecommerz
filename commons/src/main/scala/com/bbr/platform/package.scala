package com.bbr

import com.bbr.platform.domain.Branch.BranchId
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Product.ProductId
import com.bbr.platform.domain.Staff.StaffId
import com.bbr.platform.domain.Transaction.TransactionId

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

package object platform {
  implicit class UuidOpts(target: UUID) {
    implicit def toOrgId: OrgId                 = OrgId(target)
    implicit def toStaffId: StaffId             = StaffId(target)
    implicit def toBranchId: BranchId           = BranchId(target)
    implicit def toProductId: ProductId         = ProductId(target)
    implicit def toTransactionId: TransactionId = TransactionId(target)
  }

  implicit class TimeOpts(target: LocalDateTime) {
    implicit def truncateTime: LocalDateTime = target.truncatedTo(ChronoUnit.MILLIS)
  }

  def getCurrentTime: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
}
