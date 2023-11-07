package com.bbr.commerz.sales.domain.cart

import com.bbr.commerz.inventory.domain.product.ProductPayloads.Quantity
import com.bbr.commerz.sales.domain.cart.CartPayloads.{Cart, CartTotal}
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Product.ProductId
import com.bbr.platform.domain.Staff.StaffId

trait CartAlgebra[F[_]] {
  def add(userId: StaffId, productId: ProductId, quantity: Quantity): F[Unit]
  def get(orgId: OrgId, userId: StaffId): F[CartTotal]
  def delete(userId: StaffId): F[Unit]
  def removeProduct(userId: StaffId, productId: ProductId): F[Unit]
  def update(userId: StaffId, cart: Cart): F[Unit]
}
