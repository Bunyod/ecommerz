package com.bbr.commerz.sales.services

import cats.effect.Async
import com.amazonaws.services.s3.AmazonS3
import com.bbr.commerz.auth.domain.token.TokensService
import com.bbr.commerz.auth.infrastructure.{AuthRepository, StaffAuthRepository}
import com.bbr.commerz.inventory.infrastructure.postgres.{
  CategoryRepository,
  ProductImageRepository,
  ProductRepository,
  UnitRepository
}
import com.bbr.commerz.organization.infrastructure.postgres.{BranchRepository, OrganizationRepository, StaffRepository}
import com.bbr.commerz.sales.infrastructure.postgres.{AgentRepository, OrderRepository, TransactionRepository}
import com.bbr.commerz.sales.infrastructure.redis.CartRepository
import com.bbr.platform.config.Configuration.ServiceConfig
import dev.profunktor.redis4cats.RedisCommands
import doobie.util.transactor.Transactor

object Repositories {
  def make[F[_]: Async](
    cfg: ServiceConfig,
    tr: Transactor[F],
    redis: RedisCommands[F, String, String],
    tokensService: TokensService[F],
    s3Client: AmazonS3
  ): Repositories[F] = {
    val productRepository = new ProductRepository[F](tr)
    new Repositories[F](
      orgRepository = new OrganizationRepository[F](tr),
      branchRepository = new BranchRepository[F](tr),
      productRepository = new ProductRepository[F](tr),
      categoryRepository = new CategoryRepository[F](tr),
      unitRepository = new UnitRepository[F](tr),
      staffRepository = new StaffRepository[F](tr),
      s3Repo = new ProductImageRepository[F](s3Client, cfg.aws),
      authRepository = new AuthRepository[F](
        tokenExpiration = cfg.tokenExpiration,
        tokens = tokensService,
        redis = redis,
        tr = tr
      ),
      cartRepository = new CartRepository[F](productRepository, redis, cfg.cartExpiration),
      transactionRepository = new TransactionRepository[F](tr),
      staffAuthRepository = new StaffAuthRepository[F](redis),
      orderRepository = new OrderRepository[F](tr),
      agentRepository = new AgentRepository[F](tr)
    )
  }
}

sealed class Repositories[F[_]](
  val orgRepository: OrganizationRepository[F],
  val branchRepository: BranchRepository[F],
  val productRepository: ProductRepository[F],
  val categoryRepository: CategoryRepository[F],
  val unitRepository: UnitRepository[F],
  val staffRepository: StaffRepository[F],
  val s3Repo: ProductImageRepository[F],
  val authRepository: AuthRepository[F],
  val cartRepository: CartRepository[F],
  val transactionRepository: TransactionRepository[F],
  val staffAuthRepository: StaffAuthRepository[F],
  val orderRepository: OrderRepository[F],
  val agentRepository: AgentRepository[F]
)
