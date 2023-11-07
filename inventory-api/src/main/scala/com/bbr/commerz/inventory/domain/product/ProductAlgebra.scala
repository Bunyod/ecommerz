package com.bbr.commerz.inventory.domain.product

import com.bbr.commerz.inventory.domain.product.ProductPayloads._
import com.bbr.commerz.inventory.domain.category.CategoryPayloads.CategoryId
import com.bbr.commerz.inventory.domain.unit.UnitPayloads.UnitId
import com.bbr.platform.domain.Product.ProductId
import com.bbr.platform.domain.Organization.OrgId

import java.util.UUID

trait ProductAlgebra[F[_]] {
  def create(product: Product): F[Product]
  def updateById(productId: ProductId, product: Product): F[Product]
  def getById(orgId: OrgId, productId: ProductId): F[Product]
  def getAll(
    orgId: OrgId,
    categoryId: Option[CategoryId] = None,
    priceFrom: Option[Double] = None,
    priceTo: Option[Double] = None,
    name: Option[String] = None,
    code: Option[String] = None,
    limit: Option[Int] = None,
    offset: Option[Int] = None
  ): F[List[Product]]
  def deleteById(orgId: OrgId, productId: ProductId): F[String]
  def updateProductImage(id: UUID, profileImagePath: String): F[ProductId]
  def getProductImagePath(productId: ProductId): F[String]
  def checkProductExistence(orgId: OrgId, productId: ProductId): F[Boolean]
  def checkProductName(orgId: OrgId, categoryId: CategoryId, unitId: UnitId, productName: ProductName): F[Boolean]
  def decreaseProductQuantity(productId: ProductId, quantity: Quantity): F[Unit]
  def increaseProductQuantity(productId: ProductId, quantity: Quantity): F[Unit]
}
