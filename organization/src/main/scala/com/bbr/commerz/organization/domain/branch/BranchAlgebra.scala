package com.bbr.commerz.organization.domain.branch

import BranchPayloads.{Branch, BranchName}
import com.bbr.platform.domain.Branch.BranchId
import com.bbr.platform.domain.Organization.OrgId

trait BranchAlgebra[F[_]] {

  def create(branch: Branch): F[Branch]
  def getByName(branchName: BranchName, orgId: OrgId): F[Option[Branch]]
  def activateBranch(orgId: OrgId, branchName: BranchName): F[Unit]
  def getAll(orgId: OrgId, branchName: Option[BranchName]): F[List[Branch]]
  def getById(orgId: OrgId, branchId: BranchId): F[Branch]
  def updateById(branchId: BranchId, branch: Branch): F[Branch]
  def deleteById(orgId: OrgId, branchId: BranchId): F[String]
  def checkBranchExistence(branchId: BranchId): F[Boolean]
}
