package com.bbr.commerz.sales.domain.cart

import com.bbr.commerz.inventory.domain.product.ProductPayloads.Quantity
import com.bbr.commerz.sales.domain.cart.CartPayloads.{Cart, CartTotal}
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Product.ProductId
import com.bbr.platform.domain.Staff.StaffId

class CartService[F[_]](cartAlgebra: CartAlgebra[F]) {

  def add(staffId: StaffId, productId: ProductId, quantity: Quantity): F[Unit] =
    cartAlgebra.add(staffId, productId, quantity)

  def get(orgId: OrgId, staffId: StaffId): F[CartTotal] =
    cartAlgebra.get(orgId, staffId)

  def delete(staffId: StaffId): F[Unit] =
    cartAlgebra.delete(staffId)

  def removeProduct(staffId: StaffId, productId: ProductId): F[Unit] =
    cartAlgebra.removeProduct(staffId, productId)

  def update(staffId: StaffId, cart: Cart): F[Unit] =
    cartAlgebra.update(staffId, cart)

}
