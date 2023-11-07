package com.bbr.commerz.sales.domain.agent

import com.bbr.commerz.sales.domain.agent.AgentPayloads.{AgentClient, ClientId}
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.{PhoneNumber, StaffId}

trait AgentAlgebra[F[_]] {

  def create(client: AgentClient): F[AgentClient]
  def updateById(client: AgentClient): F[AgentClient]
  def getById(orgId: OrgId, agentId: StaffId, clientId: ClientId): F[AgentClient]
  def getAll(orgId: OrgId, staffId: StaffId, limit: Int, offset: Int): F[List[AgentClient]]
  def deleteById(orgId: OrgId, agentId: StaffId, clientId: ClientId): F[Unit]
  def checkExistence(orgId: OrgId, phoneNumber: PhoneNumber): F[Option[AgentClient]]
  def activateClient(orgId: OrgId, phoneNumber: PhoneNumber): F[Unit]

}
