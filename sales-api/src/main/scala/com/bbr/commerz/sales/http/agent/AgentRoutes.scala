package com.bbr.commerz.sales.http.agent

import cats.effect.Async
import cats.implicits._
import com.bbr.commerz.sales.domain.agent.AgentPayloads._
import com.bbr.commerz.sales.domain.agent.AgentService
import com.bbr.commerz.sales.http.utils.json._
import com.bbr.platform.UuidOpts
import com.bbr.platform.domain.Staff.StaffAuth
import com.bbr.platform.domain.Staff.StaffRole.AGENT
import com.bbr.platform.http.QueryParameters
import com.bbr.platform.utils.decoder.RefinedRequestDecoder
import org.http4s.circe._
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}

class AgentRoutes[F[_]: Async: JsonDecoder](
  agentService: AgentService[F]
) extends QueryParameters[F] {

  private val httpRoutes: AuthedRoutes[StaffAuth, F] = AuthedRoutes.of {

    case ar @ POST -> Root / UUIDVar(orgId) / "agent" / "client" as StaffAuth(agentId, _, _, AGENT) =>
      ar.req.decodeR[AgentClientRequest] { request =>
        Created(agentService.create(orgId.toOrgId, agentId, request))
          .recoverWith(error => InternalServerError(error.getMessage))
      }

    case ar @ PUT -> Root / UUIDVar(orgId) / "agent" / "client" / UUIDVar(id) as StaffAuth(agentId, _, _, AGENT) =>
      ar.req.decodeR[AgentClientUpdate] { request =>
        Ok(agentService.updateById(orgId.toOrgId, agentId, ClientId(id), request))
          .recoverWith(error => InternalServerError(error.getMessage))

      }

    case GET -> Root / UUIDVar(orgId) / "agent" / "client" / UUIDVar(id) as StaffAuth(agentId, _, _, AGENT) =>
      Ok(agentService.getById(orgId.toOrgId, agentId, ClientId(id)))
        .recoverWith(error => InternalServerError(error.getMessage))

    case GET -> Root / UUIDVar(orgId) / "agent" / "client" :? LimitQueryParamMatcher(
          limit
        ) +& OffsetQueryParamMatcher(
          offset
        ) as StaffAuth(agentId, _, _, AGENT) =>
      Ok(agentService.getAll(orgId.toOrgId, agentId, limit, offset))

    case DELETE -> Root / UUIDVar(orgId) / "agent" / "client" / UUIDVar(id) as StaffAuth(agentId, _, _, AGENT) =>
      agentService.deleteById(orgId.toOrgId, agentId, ClientId(id)) *> NoContent()
  }

  private[http] val pathPrefix = "/org"

  def routes(authMiddleware: AuthMiddleware[F, StaffAuth]): HttpRoutes[F] = Router(
    pathPrefix -> authMiddleware(httpRoutes)
  )
}
