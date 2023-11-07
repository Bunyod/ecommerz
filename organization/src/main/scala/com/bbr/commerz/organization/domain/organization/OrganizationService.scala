package com.bbr.commerz.organization.domain.organization

import cats._
import cats.implicits._
import OrganizationPayloads._
import com.bbr.platform.{getCurrentTime, UuidOpts}
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.PhoneNumber
import com.bbr.platform.effekts.GenUUID

class OrganizationService[F[_]: GenUUID: MonadThrow](organizationAlgebra: OrganizationAlgebra[F]) {

  def create(organization: OrganizationRequest, phoneNumber: PhoneNumber): F[Organization] =
    for {
      uuid <- GenUUID[F].make
      res  <- organizationAlgebra.create(organization.toDomain(uuid.toOrgId, getCurrentTime.some), phoneNumber)
    } yield res

  def getAll(limit: Option[Int], offset: Option[Int]): F[List[Organization]] =
    organizationAlgebra.getAll(limit, offset)

  def getById(id: OrgId): F[Organization] =
    organizationAlgebra.getById(id)

  def updateById(orgId: OrgId, organization: OrganizationRequest): F[Organization] =
    organizationAlgebra.checkOrganizationExistence(orgId).flatMap {
      case true => organizationAlgebra.updateById(orgId, organization.toDomain(orgId, None, getCurrentTime.some))
      case _    => new Throwable("Organization does not exist").raiseError[F, Organization]
    }

  def deleteById(id: OrgId): F[String] =
    organizationAlgebra.deleteById(id)

  def getAll(
    orgName: OrganizationName,
    limit: Option[Int] = None,
    offset: Option[Int] = None
  ): F[List[Organization]] =
    organizationAlgebra.getAll(orgName, limit.getOrElse(50), offset.getOrElse(0))
}
