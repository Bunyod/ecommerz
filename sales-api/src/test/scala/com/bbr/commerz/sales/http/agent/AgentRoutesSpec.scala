package com.bbr.commerz.sales.http.agent

import cats.data.Kleisli
import cats.effect._
import cats.implicits._
import com.bbr.commerz.organization.suite.HttpTestSuite
import com.bbr.commerz.sales.domain.agent.AgentPayloads._
import com.bbr.commerz.sales.domain.agent.{AgentAlgebra, AgentService}
import com.bbr.commerz.sales.suite.SaleGenerators
import com.bbr.commerz.sales.suite.json._
import com.bbr.platform.UuidOpts
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.{PhoneNumber, StaffAuth, StaffId, StaffRole}
import io.circe.syntax._
import org.http4s.Method._
import org.http4s.client.dsl.io._
import org.http4s.headers.`Content-Type`
import org.http4s.server.AuthMiddleware
import org.http4s.{HttpRoutes, MediaType, Status, Uri}

import java.util.UUID

object AgentRoutesSpec extends HttpTestSuite with SaleGenerators {

  def authMiddleware(staffAuth: StaffAuth): AuthMiddleware[IO, StaffAuth] =
    AuthMiddleware(Kleisli.pure(staffAuth))

  private val staffAuth: StaffAuth = StaffAuth(
    id = UUID.randomUUID().toStaffId,
    branchId = UUID.randomUUID().toBranchId.some,
    phoneNumber = PhoneNumber("+998907412586"),
    role = StaffRole.AGENT
  )

  private val orgId = genOrgId.sample.get.value

  def createAgent(request: AgentClient): AgentService[IO] = new AgentService[IO](
    new AgentClientRepository {
      override def create(client: AgentClient): IO[AgentClient] = IO.pure(request)
    }
  )

  def failingCreateAgent(): AgentService[IO] = new AgentService[IO](
    new AgentClientRepository {
      override def create(client: AgentClient): IO[AgentClient] = IO.raiseError(DummyError)
    }
  )

  def update(newClient: AgentClient): AgentService[IO] = new AgentService[IO](
    new AgentClientRepository {
      override def getById(orgId: OrgId, agentId: StaffId, clientId: ClientId): IO[AgentClient] = newClient.pure[IO]
      override def updateById(client: AgentClient): IO[AgentClient]                             = newClient.pure[IO]
    }
  )

  def failingUpdateClient(): AgentService[IO] = new AgentService[IO](
    new AgentClientRepository {
      override def updateById(client: AgentClient): IO[AgentClient] = IO.raiseError(DummyError)
    }
  )

  def getAgentClient(agent: AgentClient): AgentService[IO] = new AgentService[IO](
    new AgentClientRepository {
      override def getById(orgId: OrgId, agentId: StaffId, clientId: ClientId): IO[AgentClient] = IO.pure(agent)
    }
  )

  def failingGetAgentClient(): AgentService[IO] = new AgentService[IO](
    new AgentClientRepository {
      override def getById(orgId: OrgId, agentId: StaffId, clientId: ClientId): IO[AgentClient] =
        IO.raiseError(DummyError)
    }
  )

  def getById(agentClients: List[AgentClient]) = new AgentService[IO](
    new AgentClientRepository {
      override def getAll(orgId: OrgId, staffId: StaffId, limit: Int, offset: Int): IO[List[AgentClient]] =
        agentClients.pure[IO]
    }
  )

  def failingGetById(): AgentService[IO] = new AgentService[IO](
    new AgentClientRepository {
      override def getAll(orgId: OrgId, staffId: StaffId, limit: Int, offset: Int): IO[List[AgentClient]] =
        IO.raiseError(DummyError)
    }
  )

  def delete(): AgentService[IO] = new AgentService[IO](
    new AgentClientRepository {
      override def deleteById(orgId: OrgId, agentId: StaffId, clientId: ClientId): IO[Unit] = ().pure[IO]
    }
  )

  def failingDelete(): AgentService[IO] = new AgentService[IO](
    new AgentClientRepository {
      override def deleteById(orgId: OrgId, agentId: StaffId, clientId: ClientId): IO[Unit] =
        IO.raiseError(DummyError)
    }
  )

  test("POST create agent client [OK]") {
    forall(genAgentClient) { agent =>
      val request                = genAgentClientRequest.sample.get
      val req                    = POST(Uri.unsafeFromString(s"/org/$orgId/agent/client"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(request.asJson)
      val routes: HttpRoutes[IO] = new AgentRoutes[IO](createAgent(agent)).routes(authMiddleware(staffAuth))
      expectHttpBodyAndStatus(routes, req)(agent, Status.Created)
    }
  }
  test("POST create agent client [FAILURE]") {
    forall(UUID.randomUUID()) { _ =>
      val request                = genAgentClientRequest.sample.get
      val req                    = POST(Uri.unsafeFromString(s"/org/$orgId/agent/client"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(request.asJson)
      val routes: HttpRoutes[IO] = new AgentRoutes[IO](failingCreateAgent()).routes(authMiddleware(staffAuth))
      expectHttpFailure(routes, req)
    }
  }

  test("PUT update agent client [OK]") {
    forall(genAgentClient) { agent =>
      val agentClientUpdate      = genAgentClientUpdate.sample.get
      val req                    = PUT(Uri.unsafeFromString(s"/org/$orgId/agent/client/${agent.agentId.value}"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(agentClientUpdate.asJson)
      val routes: HttpRoutes[IO] = new AgentRoutes[IO](update(agent)).routes(authMiddleware(staffAuth))
      expectHttpBodyAndStatus(routes, req)(agent, Status.Ok)
    }
  }
  test("PUT update agent client [FAILURE]") {
    forall(genAgentClientUpdate) { clientUpdate =>
      val req                    = PUT(Uri.unsafeFromString(s"/org/$orgId/agent/client/${UUID.randomUUID()}"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(clientUpdate.asJson)
      val routes: HttpRoutes[IO] = new AgentRoutes[IO](failingUpdateClient()).routes(authMiddleware(staffAuth))
      expectHttpFailure(routes, req)
    }
  }

  test("GET agent client by ID [OK]") {
    forall(genAgentClient) { agent =>
      val req                    = GET(Uri.unsafeFromString(s"/org/$orgId/agent/client/${agent.agentId.value}"))
      val routes: HttpRoutes[IO] = new AgentRoutes[IO](getAgentClient(agent)).routes(authMiddleware(staffAuth))
      expectHttpStatus(routes, req)(Status.Ok)
    }
  }
  test("GET agent client by ID [FAILURE]") {
    forall(genAgentClient) { agent =>
      val req                    = GET(Uri.unsafeFromString(s"/org/$orgId/agent/client/${agent.agentId.value}"))
      val routes: HttpRoutes[IO] = new AgentRoutes[IO](failingGetAgentClient()).routes(authMiddleware(staffAuth))
      expectHttpFailure(routes, req)
    }
  }

  test("GET all agent clients [OK]") {
    forall(genAgentClients) { agents =>
      val req                    = GET(Uri.unsafeFromString(s"/org/$orgId/agent/client"))
      val routes: HttpRoutes[IO] = new AgentRoutes[IO](getById(agents)).routes(authMiddleware(staffAuth))
      expectHttpBodyAndStatus(routes, req)(agents, Status.Ok)
    }
  }

  test("GET all agent clients [FAILURE]") {
    forall(UUID.randomUUID()) { _ =>
      val req                    = GET(Uri.unsafeFromString(s"/org/$orgId/agent/client"))
      val routes: HttpRoutes[IO] = new AgentRoutes[IO](failingGetById()).routes(authMiddleware(staffAuth))
      expectHttpFailure(routes, req)
    }
  }

  test("DELETE agent client [OK]") {
    forall(genAgentClient) { agent =>
      val req                    = DELETE(Uri.unsafeFromString(s"/org/$orgId/agent/client/${agent.id.value}"))
      val routes: HttpRoutes[IO] = new AgentRoutes[IO](delete()).routes(authMiddleware(staffAuth))
      expectHttpStatus(routes, req)(Status.NoContent)
    }
  }
  test("DELETE agent client [FAILURE  ]") {
    forall(UUID.randomUUID()) { id =>
      val req                    = DELETE(Uri.unsafeFromString(s"/org/$orgId/agent/client/$id"))
      val routes: HttpRoutes[IO] = new AgentRoutes[IO](failingDelete()).routes(authMiddleware(staffAuth))
      expectHttpFailure(routes, req)
    }
  }
}

protected[http] class AgentClientRepository extends AgentAlgebra[IO] {

  override def create(client: AgentClient): IO[AgentClient] = client.pure[IO]

  override def updateById(client: AgentClient): IO[AgentClient] = client.pure[IO]

  override def getById(orgId: OrgId, agentId: StaffId, clientId: ClientId): IO[AgentClient] = IO.pure(null)

  override def getAll(
    orgId: OrgId,
    staffId: StaffId,
    limit: Int,
    offset: Int
  ): IO[List[AgentClient]] = IO.pure(List.empty[AgentClient])

  override def deleteById(orgId: OrgId, agentId: StaffId, clientId: ClientId): IO[Unit] = ().pure[IO]

  override def checkExistence(orgId: OrgId, phoneNumber: PhoneNumber): IO[Option[AgentClient]] =
    IO.pure(None)

  override def activateClient(orgId: OrgId, phoneNumber: PhoneNumber): IO[Unit] = ().pure[IO]
}
