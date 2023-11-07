package com.bbr.commerz.organization.domain.staff

import cats.implicits._
import cats.effect.Async
import StaffPayloads.{Staff, StaffRequest, StaffUpdate}
import com.bbr.platform.crypto.CryptoAlgebra
import com.bbr.platform.{getCurrentTime, UuidOpts}
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.StaffId
import com.bbr.platform.effekts.GenUUID

class StaffService[F[_]: Async: GenUUID](staffAlgebra: StaffAlgebra[F], crypto: CryptoAlgebra) {

  import StaffService._

  def create(orgId: OrgId, request: StaffRequest): F[Staff] =
    for {
      uuid <- GenUUID[F].make
      staff = request.toDomain(uuid.toStaffId, orgId, crypto.encrypt(request.password.toDomain), getCurrentTime.some)
      res  <- staffAlgebra.create(staff)
    } yield res

  def getWorkers(
    orgId: OrgId,
    username: Option[String] = None,
    limit: Option[Int] = None,
    offset: Option[Int] = None
  ): F[List[Staff]] =
    staffAlgebra.getWorkers(orgId, username, limit.getOrElse(50), offset.getOrElse(0))

  def getAgents(
    orgId: OrgId,
    username: Option[String] = None,
    limit: Option[Int] = None,
    offset: Option[Int] = None
  ): F[List[Staff]] =
    staffAlgebra.getAgents(orgId, username, limit.getOrElse(50), offset.getOrElse(0))

  def getById(orgId: OrgId, staffId: StaffId): F[Staff] =
    staffAlgebra.getById(orgId, staffId)

  def updateById(orgId: OrgId, staffId: StaffId, staff: StaffUpdate): F[Staff] =
    for {
      oldStaff   <- getById(orgId, staffId)
      staffUpdate = buildUpdateBody(oldStaff, staff)
      staff      <- staffAlgebra.updateById(staffId, staffUpdate)
    } yield staff

  def deleteById(orgId: OrgId, staffId: StaffId): F[String] =
    staffAlgebra.deleteById(orgId, staffId)
}

object StaffService {

  private def buildUpdateBody(old: Staff, request: StaffUpdate): Staff =
    old.copy(
      branchId = request.branchId,
      userName = request.userName.fold(old.userName)(_.toDomain),
      phoneNumber = request.phoneNumber.fold(old.phoneNumber)(_.toDomain),
      firstName = request.firstName,
      lastName = request.lastName,
      birthDate = request.birthDate
    )

}
