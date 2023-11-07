package com.bbr.commerz.sales.services

import cats.effect.Async
import com.bbr.commerz.auth.domain.auth.{AuthService, UserAuthService}
import com.bbr.commerz.inventory.domain.category.CategoryService
import com.bbr.commerz.inventory.domain.image.product.ProductImageService
import com.bbr.commerz.inventory.domain.product.ProductService
import com.bbr.commerz.inventory.domain.unit.UnitService
import com.bbr.commerz.organization.domain.branch.BranchService
import com.bbr.commerz.organization.domain.organization.OrganizationService
import com.bbr.commerz.organization.domain.staff.StaffService
import com.bbr.commerz.sales.domain.agent.AgentService
import com.bbr.commerz.sales.domain.order.OrderService
import com.bbr.commerz.sales.domain.transaction.TransactionService
import com.bbr.commerz.sales.domain.cart.CartService
import com.bbr.platform.crypto.CryptoAlgebra
import com.bbr.platform.domain.Staff.StaffAuth

object Services {
  def make[F[_]: Async](
    repositories: Repositories[F],
    cryptoService: CryptoAlgebra
  ) =
    new Services[F](
      organization = new OrganizationService[F](repositories.orgRepository),
      branch = new BranchService[F](repositories.branchRepository),
      product = new ProductService[F](repositories.productRepository),
      category = new CategoryService[F](repositories.categoryRepository),
      unit = new UnitService[F](repositories.unitRepository),
      staff = new StaffService[F](repositories.staffRepository, cryptoService),
      productImage = new ProductImageService[F](repositories.s3Repo, repositories.productRepository),
      auth = new AuthService[F](repositories.authRepository, cryptoService),
      cart = new CartService[F](repositories.cartRepository),
      transaction = new TransactionService[F](repositories.transactionRepository),
      userAuthService = new UserAuthService(repositories.staffAuthRepository),
      order = new OrderService[F](repositories.orderRepository, repositories.productRepository),
      agent = new AgentService[F](repositories.agentRepository)
    )
}
sealed class Services[F[_]](
  val organization: OrganizationService[F],
  val branch: BranchService[F],
  val product: ProductService[F],
  val category: CategoryService[F],
  val unit: UnitService[F],
  val staff: StaffService[F],
  val productImage: ProductImageService[F],
  val auth: AuthService[F],
  val cart: CartService[F],
  val transaction: TransactionService[F],
  val userAuthService: UserAuthService[F, StaffAuth],
  val order: OrderService[F],
  val agent: AgentService[F]
)
