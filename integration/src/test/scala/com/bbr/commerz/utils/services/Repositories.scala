package com.bbr.commerz.utils.services

import cats.effect.Async
import com.bbr.commerz.inventory.infrastructure.postgres.{CategoryRepository, ProductRepository, UnitRepository}
import com.bbr.commerz.organization.infrastructure.postgres.{BranchRepository, OrganizationRepository, StaffRepository}
import com.bbr.commerz.sales.infrastructure.postgres.{AgentRepository, OrderRepository, TransactionRepository}
import doobie.util.transactor.Transactor

object Repositories {
  def make[F[_]: Async](
    tr: Transactor[F]
  ): Repositories[F] = {

    val staffRepository = new StaffRepository[F](tr)

    new Repositories[F](
      orgRepo = new OrganizationRepository[F](tr),
      branchRepo = new BranchRepository[F](tr),
      productRepo = new ProductRepository[F](tr),
      categoryRepo = new CategoryRepository[F](tr),
      unitRepo = new UnitRepository[F](tr),
      staffRepo = staffRepository,
      orderRepo = new OrderRepository[F](tr),
      transactionRepo = new TransactionRepository[F](tr),
      agentClientRepo = new AgentRepository[F](tr)
    )
  }
}

sealed class Repositories[F[_]](
  val orgRepo: OrganizationRepository[F],
  val branchRepo: BranchRepository[F],
  val productRepo: ProductRepository[F],
  val categoryRepo: CategoryRepository[F],
  val unitRepo: UnitRepository[F],
  val staffRepo: StaffRepository[F],
  val orderRepo: OrderRepository[F],
  val transactionRepo: TransactionRepository[F],
  val agentClientRepo: AgentRepository[F]
)
