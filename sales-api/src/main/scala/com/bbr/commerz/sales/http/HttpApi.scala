package com.bbr.commerz.sales.http

import cats.effect._
import cats.implicits._
import com.bbr.commerz.auth.http.auth.{AuthRoutes, LoginRoutes, LogoutRoutes}
import com.bbr.commerz.inventory.http.category.CategoryRoutes
import com.bbr.commerz.inventory.http.product.ProductRoutes
import com.bbr.commerz.inventory.http.unit.UnitRoutes
import com.bbr.commerz.organization.domain.staff.StaffPayloads.UserJwtAuth
import com.bbr.commerz.organization.http.branch.BranchRoutes
import com.bbr.commerz.organization.http.organization.OrganizationRoutes
import com.bbr.commerz.organization.http.staff.{OwnerRoutes, StaffRoutes}
import com.bbr.commerz.sales.http.agent.AgentRoutes
import com.bbr.commerz.sales.http.order.OrderRoutes
import com.bbr.commerz.sales.http.transaction.TransactionRoutes
import com.bbr.commerz.sales.http.cart.CartRoutes
import com.bbr.commerz.sales.services.Services
import com.bbr.platform.domain.Staff.StaffAuth
import dev.profunktor.auth.JwtAuthMiddleware
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.server.middleware.{RequestLogger, ResponseLogger}
import org.http4s.{HttpApp, HttpRoutes}

class HttpApi[F[_]: Async](
  services: Services[F],
  userJwtAuth: UserJwtAuth
) {

  private val authMiddleware: AuthMiddleware[F, StaffAuth] =
    JwtAuthMiddleware[F, StaffAuth](userJwtAuth.value, services.userAuthService.findUser)

  private val loginRoutes  = new LoginRoutes[F](services.auth).routes
  private val logoutRoutes = new LogoutRoutes[F](services.auth).routes(authMiddleware)
  private val authRoutes   = new AuthRoutes[F](services.auth).routes(authMiddleware)

  private val ownerRoutes  = new OwnerRoutes[F](services.staff).routes(authMiddleware)
  private val workerRoutes = new StaffRoutes[F](services.staff).routes(authMiddleware)

  private val unitRoutes     = new UnitRoutes[F](services.unit).routes(authMiddleware)
  private val categoryRoutes = new CategoryRoutes[F](services.category).routes(authMiddleware)
  private val productRoutes  = new ProductRoutes[F](services.product, services.productImage).routes(authMiddleware)

  private val branchRoutes       = new BranchRoutes[F](services.branch).routes(authMiddleware)
  private val organizationRoutes = new OrganizationRoutes[F](services.organization).routes(authMiddleware)

  private val cartRoutes: HttpRoutes[F]        = new CartRoutes[F](services.cart).routes(authMiddleware)
  private val transactionRoutes: HttpRoutes[F] = new TransactionRoutes[F](services.transaction).routes(authMiddleware)
  private val orderRoutes: HttpRoutes[F]       = new OrderRoutes[F](services.order, services.cart).routes(authMiddleware)
  private val agentRoutes: HttpRoutes[F]       = new AgentRoutes[F](services.agent).routes(authMiddleware)

  private val openRoutes: HttpRoutes[F] = loginRoutes

  private val authedRoutes: HttpRoutes[F] =
    categoryRoutes <+> unitRoutes <+> productRoutes <+> branchRoutes <+> organizationRoutes <+> logoutRoutes <+>
      ownerRoutes <+> workerRoutes <+> agentRoutes <+> cartRoutes <+> orderRoutes <+> transactionRoutes <+> authRoutes

  val routes: HttpRoutes[F] = Router(
    version.v1 + "/auth" -> openRoutes,
    version.v1           -> authedRoutes
  )

  private val loggers: HttpApp[F] => HttpApp[F] = { http: HttpApp[F] =>
    RequestLogger.httpApp(logHeaders = true, logBody = true)(http)
  }.andThen { http: HttpApp[F] =>
    ResponseLogger.httpApp(logHeaders = true, logBody = true)(http)
  }

  val httpApp: HttpApp[F] = loggers(routes.orNotFound)
}

object version {
  val v1 = "/v1"
}
