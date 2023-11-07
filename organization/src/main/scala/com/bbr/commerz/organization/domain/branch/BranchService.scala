package com.bbr.commerz.organization.domain.branch

import cats.MonadThrow
import cats.implicits._
import com.bbr.platform.domain.Branch.BranchId
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.StaffId
import com.bbr.platform.effekts.GenUUID
import com.bbr.platform.getCurrentTime

class BranchService[F[_]: GenUUID: MonadThrow](branchAlgebra: BranchAlgebra[F]) {

  import BranchPayloads._

  def create(orgId: OrgId, branch: BranchRequest, staffId: StaffId): F[Branch] =
    for {
      uuid <- GenUUID[F].make
      brn  <- branchAlgebra.getByName(branch.name.toDomain, orgId)
      res  <- brn match {
                case None                                         =>
                  branchAlgebra.create(branch.toDomain(BranchId(uuid), orgId, staffId, getCurrentTime.some))
                case Some(b) if b.status == BranchStatus.INACTIVE =>
                  branchAlgebra
                    .activateBranch(orgId, branch.name.toDomain) *> b.copy(status = BranchStatus.ACTIVE).pure[F]
                case _                                            =>
                  new Throwable(s"The branch already exists: ${branch.name.value}").raiseError[F, Branch]
              }
    } yield res

  def updateById(orgId: OrgId, branchId: BranchId, request: BranchUpdate): F[Branch] =
    branchAlgebra.checkBranchExistence(branchId).flatMap {
      case true =>
        for {
          oldBranch <- getById(orgId, branchId)
          updated    = oldBranch.copy(
                         branchName = request.name.map(_.toDomain).getOrElse(oldBranch.branchName),
                         address = request.address.fold(oldBranch.address)(identity)
                       )
          branch    <- branchAlgebra.updateById(branchId, updated)
        } yield branch
      case _    =>
        new Throwable("The branch does not exist or the new branch name is occupied").raiseError[F, Branch]

    }

  def getById(orgId: OrgId, branchId: BranchId): F[Branch] =
    branchAlgebra.getById(orgId, branchId)

  def getAll(orgId: OrgId, branchName: Option[BranchName]): F[List[Branch]] =
    branchAlgebra.getAll(orgId, branchName)

  def deleteById(orgId: OrgId, branchId: BranchId): F[String] =
    branchAlgebra.deleteById(orgId, branchId)
}
