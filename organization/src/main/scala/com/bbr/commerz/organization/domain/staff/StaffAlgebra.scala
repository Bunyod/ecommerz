package com.bbr.commerz.organization.domain.staff

import StaffPayloads.Staff
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.{PhoneNumber, StaffId}

trait StaffAlgebra[F[_]] {
  def create(staff: Staff): F[Staff]
  def getWorkers(orgId: OrgId, username: Option[String] = None, limit: Int, offset: Int): F[List[Staff]]
  def getAgents(orgId: OrgId, username: Option[String] = None, limit: Int, offset: Int): F[List[Staff]]
  def getAll(orgId: OrgId, username: Option[String] = None, limit: Int, offset: Int): F[List[Staff]]
  def getById(orgId: OrgId, id: StaffId): F[Staff]
  def updateById(staffId: StaffId, staff: Staff): F[Staff]
  def deleteById(orgId: OrgId, id: StaffId): F[String]
  def checkPhoneNumber(orgId: OrgId, phoneNumber: PhoneNumber): F[Boolean]
}
