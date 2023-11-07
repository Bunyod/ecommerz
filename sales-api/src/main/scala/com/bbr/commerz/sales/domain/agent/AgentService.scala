package com.bbr.commerz.sales.domain.agent

import cats.effect.Async
import cats.implicits._
import com.bbr.commerz.sales.domain.agent.AgentPayloads._
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.StaffId
import com.bbr.platform.effekts.GenUUID
import com.bbr.platform.getCurrentTime

class AgentService[F[_]: Async: GenUUID](repository: AgentAlgebra[F]) {

  import AgentService._

  def create(orgId: OrgId, agentId: StaffId, request: AgentClientRequest): F[AgentClient] =
    for {
      uuid   <- GenUUID[F].make
      check  <- repository.checkExistence(orgId, request.phoneNumber.toDomain)
      result <- checkAndCreate(orgId, ClientId(uuid), agentId, request, check, repository)
    } yield result

  def updateById(orgId: OrgId, agentId: StaffId, clientId: ClientId, request: AgentClientUpdate): F[AgentClient] =
    for {
      oldClient <- repository.getById(orgId, agentId, clientId)
      newBody    = buildUpdateBody(oldClient, agentId, request)
      updated   <- repository.updateById(newBody)
    } yield updated

  def getById(orgId: OrgId, agentId: StaffId, clientId: ClientId): F[AgentClient] =
    repository.getById(orgId, agentId, clientId)

  def getAll(orgId: OrgId, staffId: StaffId, limit: Option[Int], offset: Option[Int]): F[List[AgentClient]] =
    repository.getAll(orgId, staffId, limit.getOrElse(50), offset.getOrElse(0))

  def deleteById(orgId: OrgId, agentId: StaffId, clientId: ClientId): F[Unit] =
    repository.deleteById(orgId, agentId, clientId)

}

object AgentService {

  private def checkAndCreate[F[_]: Async](
    orgId: OrgId,
    clientId: ClientId,
    agentId: StaffId,
    request: AgentClientRequest,
    maybeClient: Option[AgentClient],
    repository: AgentAlgebra[F]
  ): F[AgentClient] =
    maybeClient match {
      case None                                         =>
        repository.create(request.toDomain(clientId, agentId, orgId, getCurrentTime.some))
      case Some(c) if c.status == ClientStatus.INACTIVE =>
        repository.activateClient(orgId, request.phoneNumber.toDomain) *> c.copy(status = ClientStatus.ACTIVE).pure[F]
      case _                                            =>
        new Throwable(s"The client does not exist in this organization.").raiseError[F, AgentClient]
    }

  private def buildUpdateBody(
    old: AgentClient,
    agentId: StaffId,
    request: AgentClientUpdate
  ): AgentClient =
    old.copy(
      phoneNumber = request.phoneNumber.map(_.toDomain).getOrElse(old.phoneNumber),
      firstName = request.firstName.map(_.toDomain).getOrElse(old.firstName),
      lastName = request.lastName.map(_.toDomain).getOrElse(old.lastName),
      address = request.address.getOrElse(old.address),
      updatedBy = agentId.some,
      updatedAt = getCurrentTime.some
    )

}
