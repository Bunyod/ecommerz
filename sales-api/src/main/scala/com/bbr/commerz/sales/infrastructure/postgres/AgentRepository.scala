package com.bbr.commerz.sales.infrastructure.postgres

import cats.effect.Async
import cats.implicits._
import com.bbr.commerz.organization.http.utils.json.addressEncoder
import com.bbr.commerz.sales.domain.agent.AgentAlgebra
import com.bbr.commerz.sales.domain.agent.AgentPayloads._
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.{PhoneNumber, StaffId}
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._             // DON'T REMOVE IT
import doobie.implicits.javasql._              // DON'T REMOVE IT
import doobie.postgres.circe.jsonb.implicits._ // DON'T REMOVE IT
import doobie.util.update.Update0
import io.circe.syntax._

import java.sql.Timestamp

class AgentRepository[F[_]: Async](tr: Transactor[F]) extends AgentAlgebra[F] {

  import AgentRepository._

  override def create(client: AgentClient): F[AgentClient] =
    insert(client).run.transact(tr) *> getById(client.orgId, client.agentId, client.id)

  override def updateById(client: AgentClient): F[AgentClient] =
    update(client).run.transact(tr) *> getById(client.orgId, client.agentId, client.id)

  override def getById(orgId: OrgId, agentId: StaffId, clientId: ClientId): F[AgentClient]           =
    selectById(orgId, agentId, clientId).option.transact(tr).flatMap {
      case Some(client) => Async[F].fromEither(client)
      case None         => new Throwable(s"Couldn't find agent client. ID: ${clientId.value}").raiseError[F, AgentClient]
    }
  override def getAll(orgId: OrgId, staffId: StaffId, limit: Int, offset: Int): F[List[AgentClient]] =
    selectAll(orgId, staffId, limit, offset).to[List].transact(tr).flatMap(_.traverse(Async[F].fromEither))

  override def deleteById(orgId: OrgId, agentId: StaffId, clientId: ClientId): F[Unit] =
    delete(orgId, agentId, clientId).run.transact(tr).void

  override def checkExistence(orgId: OrgId, phoneNumber: PhoneNumber): F[Option[AgentClient]] =
    checkClient(orgId, phoneNumber).option.transact(tr).flatMap(_.traverse(Async[F].fromEither))

  override def activateClient(orgId: OrgId, phoneNumber: PhoneNumber): F[Unit] =
    activate(orgId, phoneNumber).run.transact(tr).void

}

object AgentRepository {

  import Drivers._

  private def insert(client: AgentClient): Update0 =
    sql"""
          INSERT INTO AGENT_CLIENT (
            ID,
            AGENT_ID,
            ORG_ID,
            PHONE_NUMBER,
            FIRST_NAME,
            LAST_NAME,
            ADDRESS,
            STATUS,
            CREATED_BY,
            UPDATED_BY,
            CREATED_AT,
            UPDATED_AT
          ) VALUES (
            ${client.id.value},
            ${client.agentId.value},
            ${client.orgId.value},
            ${client.phoneNumber.value},
            ${client.firstName.value},
            ${client.lastName.value},
            ${client.address.asJson},
            ${client.status.entryName},
            ${client.createdBy.value},
            ${client.updatedBy.map(_.value)},
            ${client.createdAt.map(Timestamp.valueOf)},
            ${client.updatedAt.map(Timestamp.valueOf)}
          )
       """.update

  private def update(client: AgentClient): Update0 =
    sql"""
          UPDATE AGENT_CLIENT SET
          PHONE_NUMBER = ${client.phoneNumber.value},
          FIRST_NAME = ${client.firstName.value},
          LAST_NAME = ${client.lastName.value},
          ADDRESS = ${client.address.asJson},
          UPDATED_BY = ${client.updatedBy.map(_.value)},
          UPDATED_AT = ${client.updatedAt.map(Timestamp.valueOf)}
          WHERE ID = ${client.id} AND
          ORG_ID = ${client.orgId.value} AND
          AGENT_ID = ${client.agentId.value}
       """.update

  private def selectById(orgId: OrgId, agentId: StaffId, clientId: ClientId): Query0[Either[Throwable, AgentClient]] =
    sql"""
          SELECT * FROM AGENT_CLIENT
          WHERE ID = ${clientId.value} AND
          ORG_ID = ${orgId.value} AND
          AGENT_ID = ${agentId.value} AND
          STATUS = ${ClientStatus.ACTIVE.entryName}
        """.query

  private def selectAll(
    orgId: OrgId,
    agentId: StaffId,
    limit: Int,
    offset: Int
  ): Query0[Either[Throwable, AgentClient]] =
    sql"""
          SELECT * FROM AGENT_CLIENT
          WHERE ORG_ID = ${orgId.value} AND
          AGENT_ID = ${agentId.value}
          LIMIT $limit
          OFFSET $offset
       """.query

  private def delete(orgId: OrgId, agentId: StaffId, clientId: ClientId): Update0 =
    sql"""
          UPDATE AGENT_CLIENT SET
          STATUS = ${ClientStatus.INACTIVE.entryName} WHERE
          ID = ${clientId.value}  AND
          ORG_ID = ${orgId.value} AND
          AGENT_ID = ${agentId.value} AND
          STATUS = ${ClientStatus.ACTIVE.entryName}
       """.update

  private def checkClient(orgId: OrgId, phoneNumber: PhoneNumber): Query0[Either[Throwable, AgentClient]] =
    sql"""
          SELECT * FROM AGENT_CLIENT WHERE
          ORG_ID= ${orgId.value} AND
          PHONE_NUMBER = ${phoneNumber.value} AND
          STATUS = ${ClientStatus.ACTIVE.entryName}
       """.query

  private def activate(orgId: OrgId, phoneNumber: PhoneNumber): Update0 =
    sql"""
          UPDATE AGENT_CLIENT SET
          STATUS = ${ClientStatus.ACTIVE.entryName} WHERE
          ORG_ID = ${orgId.value} AND
          PHONE_NUMBER = ${phoneNumber.value}
       """.update

}
