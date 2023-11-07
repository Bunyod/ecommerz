package com.bbr.commerz.organization.domain.organization

import OrganizationPayloads.{Organization, OrganizationName}
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.PhoneNumber

trait OrganizationAlgebra[F[_]] {
  def create(organization: Organization, phoneNumber: PhoneNumber): F[Organization]
  def getAll(limit: Option[Int], offset: Option[Int]): F[List[Organization]]
  def getById(id: OrgId): F[Organization]
  def updateById(orgId: OrgId, organization: Organization): F[Organization]
  def deleteById(id: OrgId): F[String]
  def getAll(orgName: OrganizationName, limit: Int, offset: Int): F[List[Organization]]
  def checkOrganizationExistence(orgId: OrgId): F[Boolean]
}
