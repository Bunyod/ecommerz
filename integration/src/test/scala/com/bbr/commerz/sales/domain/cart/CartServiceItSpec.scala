package com.bbr.commerz.sales.domain.cart

import cats.effect.IO
import cats.effect.Resource
import com.bbr.commerz.organization.domain.owner.OwnerPayloads.Owner
import com.bbr.commerz.sales.domain.cart.CartPayloads.Cart
import com.bbr.commerz.sales.infrastructure.redis.CartRepository
import com.bbr.commerz.utils.ConfigOverrideChecks
import com.bbr.commerz.utils.ItUtils.{config, createOwner, repositories, services}
import com.bbr.commerz.utils.ItUtils.config.cartExpiration
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.NoOp.instance

object CartServiceItSpec extends ConfigOverrideChecks {

  override type Res = CartService[IO]

  override def sharedResource: Resource[IO, Res] = Redis[IO]
    .utf8(config.redis.uri.value)
    .map { redis =>
      val cartRepository = new CartRepository[IO](repositories.productRepo, redis, cartExpiration)
      new CartService[IO](cartRepository)
    }
  private val owner: Owner                       = createOwner(genPhoneNumber.sample.get)

  test("Test all cart combinations [SUCCESS]") { cartService =>
    forall(genAgent) { worker =>
      for {
        orgId        <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        staffId      <- repositories.staffRepo.create(worker.copy(orgId = orgId)).map(_.id)
        categoryId   <- services.category.create(orgId, genCategoryRequest.sample.get).map(_.id)
        unitId       <- services.unit.create(orgId, genUnitRequest.sample.get).map(_.id)
        p1           <- repositories.productRepo.create(
                          genProduct.sample.get
                            .copy(orgId = orgId, unitId = unitId, categoryId = categoryId, branchId = None)
                        )
        p2           <- repositories.productRepo.create(
                          genProduct.sample.get
                            .copy(orgId = orgId, unitId = unitId, categoryId = categoryId, branchId = None)
                        )
        x            <- cartService.get(orgId, staffId)
        (q1, q2)      = (genQuantity.sample.get, genQuantity.sample.get)
        _            <- cartService.add(staffId, p1.id, q1)
        _            <- cartService.add(staffId, p2.id, q1)
        y            <- cartService.get(orgId, staffId)
        _            <- cartService.removeProduct(staffId, p1.id)
        z            <- cartService.get(orgId, staffId)
        _            <- cartService.update(staffId, Cart(Map(p2.id -> q2)))
        w            <- cartService.get(orgId, staffId)
        _            <- cartService.delete(staffId)
        v            <- cartService.get(orgId, staffId)
        verifications = expect.all(
                          x.products.isEmpty,
                          y.products.size == 2,
                          z.products.size == 1,
                          v.products.isEmpty,
                          w.products.headOption.fold(false)(_.quantity == q2)
                        )
      } yield verifications
    }
  }
}
