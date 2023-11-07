package com.bbr.commerz.utils.services

import cats.effect.Async
import com.bbr.commerz.inventory.domain.category.CategoryService
import com.bbr.commerz.inventory.domain.product.ProductService
import com.bbr.commerz.inventory.domain.unit.UnitService
import com.bbr.commerz.organization.domain.branch.BranchService
import com.bbr.commerz.organization.domain.organization.OrganizationService
import com.bbr.commerz.organization.domain.staff.StaffService
import com.bbr.commerz.sales.domain.agent.AgentService
import com.bbr.commerz.sales.domain.order.OrderService
import com.bbr.commerz.sales.domain.transaction.TransactionService
import com.bbr.platform.crypto.CryptoAlgebra

object Services {
  def make[F[_]: Async](
    repositories: Repositories[F],
    cryptoAlgebra: CryptoAlgebra
  ) =
    new Services[F](
      organization = new OrganizationService[F](repositories.orgRepo),
      branch = new BranchService[F](repositories.branchRepo),
      product = new ProductService[F](repositories.productRepo),
      category = new CategoryService[F](repositories.categoryRepo),
      unit = new UnitService[F](repositories.unitRepo),
      staff = new StaffService[F](repositories.staffRepo, cryptoAlgebra),
      order = new OrderService[F](repositories.orderRepo, repositories.productRepo),
      transaction = new TransactionService[F](repositories.transactionRepo),
      agent = new AgentService[F](repositories.agentClientRepo)
    )
}

sealed class Services[F[_]](
  val organization: OrganizationService[F],
  val branch: BranchService[F],
  val product: ProductService[F],
  val category: CategoryService[F],
  val unit: UnitService[F],
  val staff: StaffService[F],
  val order: OrderService[F],
  val transaction: TransactionService[F],
  val agent: AgentService[F]
)
