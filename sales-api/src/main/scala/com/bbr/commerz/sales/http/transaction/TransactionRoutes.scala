package com.bbr.commerz.sales.http.transaction

import cats.effect.Async
import cats.implicits._
import com.bbr.commerz.sales.domain.order.OrderPayloads.OrderId
import com.bbr.commerz.sales.domain.transaction.TransactionPayloads._
import com.bbr.commerz.sales.domain.transaction.TransactionService
import com.bbr.commerz.sales.http.utils.json._
import com.bbr.platform.UuidOpts
import com.bbr.platform.domain.Staff.StaffAuth
import com.bbr.platform.domain.Staff.StaffRole.OWNER
import com.bbr.platform.domain.Transaction.TransactionId
import com.bbr.platform.http.QueryParameters
import com.bbr.platform.utils.decoder._
import org.http4s.circe._
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}

final class TransactionRoutes[F[_]: Async: JsonDecoder](
  transactionService: TransactionService[F]
) extends QueryParameters[F] {

  private val httpRoutes: AuthedRoutes[StaffAuth, F] = AuthedRoutes.of {

    case ar @ POST -> Root / UUIDVar(orgId) / "transaction" as StaffAuth(staffId, _, _, _) =>
      ar.req.decodeR[TransactionRequest] { tr =>
        Created(transactionService.create(orgId.toOrgId, staffId, tr))
          .recoverWith(er => InternalServerError(er.getMessage))
      }

    case GET -> Root / UUIDVar(orgId) / "transaction" / UUIDVar(id) as StaffAuth(_, _, _, OWNER) =>
      Ok(transactionService.getById(orgId.toOrgId, TransactionId(id)))
        .recoverWith(er => InternalServerError(er.getMessage))

    case GET -> Root / UUIDVar(orgId) / "transaction" :? OrderIdQueryParamMatcher(
          orderId
        ) +& OffsetQueryParamMatcher(
          offset
        ) +& LimitQueryParamMatcher(
          limit
        ) as StaffAuth(_, _, _, OWNER) =>
      Ok(transactionService.getAll(orgId.toOrgId, orderId.map(OrderId.apply), limit, offset))
        .recoverWith(er => InternalServerError(er.getMessage))

    case DELETE -> Root / UUIDVar(orgId) / "transaction" / UUIDVar(id) as StaffAuth(_, _, _, OWNER) =>
      Ok(transactionService.deleteById(orgId.toOrgId, TransactionId(id)))
        .recoverWith(er => InternalServerError(er.getMessage))

  }

  private[http] val pathPrefix = "/org"

  def routes(authMiddleware: AuthMiddleware[F, StaffAuth]): HttpRoutes[F] = Router(
    pathPrefix -> authMiddleware(httpRoutes)
  )

}
