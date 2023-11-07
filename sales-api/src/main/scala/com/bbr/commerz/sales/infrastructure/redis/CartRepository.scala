package com.bbr.commerz.sales.infrastructure.redis

import cats.effect.Sync
import cats.{MonadThrow, Monoid}
import cats.implicits._
import dev.profunktor.redis4cats.RedisCommands
import com.bbr.commerz.organization.http.utils.json.showStaffId
import com.bbr.commerz.inventory.domain.product.ProductPayloads.Quantity
import com.bbr.commerz.inventory.domain.product.ProductAlgebra
import com.bbr.commerz.sales.domain.cart.CartPayloads._
import com.bbr.commerz.sales.domain.cart.CartAlgebra
import com.bbr.platform.config.Configuration.CartExpiration
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Product.ProductId
import com.bbr.platform.domain.Staff.StaffId
import com.bbr.platform.domain.Product
import com.bbr.platform.effekts.GenUUID
import squants.market.{Money, USD}

class CartRepository[F[_]: Sync](
  productAlgebra: ProductAlgebra[F],
  redis: RedisCommands[F, String, String],
  expiration: CartExpiration
) extends CartAlgebra[F] {

  override def add(staffId: StaffId, productId: ProductId, quantity: Quantity): F[Unit] =
    redis.hSet(staffId.show, productId.value.toString, quantity.value.toString) *>
      redis.expire(staffId.show, expiration.value).void

  implicit val moneyMonoid: Monoid[Money] =
    new Monoid[Money] {
      def empty: Money                       = USD(0)
      def combine(x: Money, y: Money): Money = x + y
    }

  override def get(orgId: OrgId, staffId: StaffId): F[CartTotal] =
    redis.hGetAll(staffId.show).flatMap { vl =>
      vl.toList
        .traverse { case (k, v) =>
          for {
            id <- GenUUID[F].read(k)
            qt <- MonadThrow[F].catchNonFatal(Quantity(v.toInt))
            rs <- productAlgebra.getById(orgId, ProductId(id)).map(v => v.cart(qt))
          } yield rs
        }
        .map { products =>
          CartTotal(products, products.foldMap(_.subtotal))
        }
    }

  override def delete(staffId: StaffId): F[Unit] =
    redis.del(staffId.show).void

  override def removeProduct(staffId: StaffId, productId: Product.ProductId): F[Unit] =
    redis.hDel(staffId.show, productId.value.toString).void

  override def update(staffId: StaffId, cart: Cart): F[Unit] =
    redis.hGetAll(staffId.show).flatMap {
      _.toList.traverse_ { case (k, _) =>
        GenUUID[F].read(k).flatMap { id =>
          cart.products.get(ProductId(id)).traverse_ { q =>
            redis.hSet(staffId.show, k, q.value.toString)
          }
        }
      }
    }
}
