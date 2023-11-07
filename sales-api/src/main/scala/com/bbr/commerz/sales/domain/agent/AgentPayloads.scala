package com.bbr.commerz.sales.domain.agent

import com.bbr.commerz.organization.domain.staff.StaffPayloads.{FirstName, FirstNameParam, LastName, LastNameParam}
import com.bbr.platform.domain.Address.Address
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.{PhoneNumber, PhoneNumberParam, StaffId}
import enumeratum._

import java.time.LocalDateTime
import java.util.UUID

object AgentPayloads {

  case class ClientId(value: UUID)

  case class AgentClientRequest(
    phoneNumber: PhoneNumberParam,
    firstName: FirstNameParam,
    lastName: LastNameParam,
    address: Address
  ) {
    def toDomain(
      id: ClientId,
      agentId: StaffId,
      orgId: OrgId,
      createdAt: Option[LocalDateTime] = None,
      updatedAt: Option[LocalDateTime] = None,
      updatedBy: Option[StaffId] = None
    ): AgentClient =
      AgentClient(
        id = id,
        agentId = agentId,
        orgId = orgId,
        phoneNumber = phoneNumber.toDomain,
        firstName = firstName.toDomain,
        lastName = lastName.toDomain,
        address = address,
        status = ClientStatus.ACTIVE,
        createdBy = agentId,
        updatedBy = updatedBy,
        createdAt = createdAt,
        updatedAt = updatedAt
      )
  }

  case class AgentClient(
    id: ClientId,
    agentId: StaffId,
    orgId: OrgId,
    phoneNumber: PhoneNumber,
    firstName: FirstName,
    lastName: LastName,
    address: Address,
    status: ClientStatus,
    createdBy: StaffId,
    updatedBy: Option[StaffId],
    createdAt: Option[LocalDateTime],
    updatedAt: Option[LocalDateTime]
  )

  case class AgentClientUpdate(
    phoneNumber: Option[PhoneNumberParam],
    firstName: Option[FirstNameParam],
    lastName: Option[LastNameParam],
    address: Option[Address]
  )

  sealed trait ClientStatus extends EnumEntry
  object ClientStatus       extends Enum[ClientStatus] with CirceEnum[ClientStatus] {
    val values: IndexedSeq[ClientStatus] = findValues
    case object ACTIVE   extends ClientStatus
    case object INACTIVE extends ClientStatus
  }
}
