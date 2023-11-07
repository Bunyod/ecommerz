package com.bbr.commerz.organization.domain.owner

import com.bbr.commerz.organization.domain.staff.StaffPayloads.{Email, FirstName, LastName, StaffStatus}
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.{EncryptedPassword, PhoneNumber, StaffId, StaffRole}

import java.time.{LocalDate, LocalDateTime}

object OwnerPayloads {

  case class Owner(
    id: StaffId,
    organizations: List[OrgId],
    role: StaffRole,
    password: EncryptedPassword,
    email: Option[Email],
    phoneNumber: PhoneNumber,
    firstName: Option[FirstName],
    lastName: Option[LastName],
    birthDate: Option[LocalDate],
    status: StaffStatus,
    createdAt: Option[LocalDateTime]
  )

}
