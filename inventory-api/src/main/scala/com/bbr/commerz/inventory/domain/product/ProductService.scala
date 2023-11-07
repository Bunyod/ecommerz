package com.bbr.commerz.inventory.domain.product

import cats.effect.Async
import cats.implicits._
import com.bbr.commerz.inventory.domain.product.ProductPayloads._
import com.bbr.platform.domain.Branch.BranchId
import com.bbr.platform.domain.Product.ProductId
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.effekts.GenUUID

class ProductService[F[_]: Async: GenUUID](productAlgebra: ProductAlgebra[F]) {

  import ProductService._

  def create(
    orgId: OrgId,
    branchId: Option[BranchId] = None,
    product: ProductRequest
  ): F[Product] =
    for {
      uuid  <- GenUUID[F].make
      check <- productAlgebra.checkProductName(orgId, product.categoryId, product.unitId, product.name.toDomain)
      res   <-
        if (check)
          new Throwable("The product name already exists or the organization/category not found.")
            .raiseError[F, Product]
        else
          productAlgebra.create(
            product.toDomain(ProductId(uuid), orgId, branchId)
          )
    } yield res

  def updateById(orgId: OrgId, productId: ProductId, request: ProductUpdate): F[Product] =
    productAlgebra
      .checkProductExistence(orgId, productId)
      .flatMap {
        case true =>
          for {
            oldProduct   <- getById(orgId, productId)
            productUpdate = buildUpdateBody(oldProduct, request)
            product      <- productAlgebra.updateById(productId, productUpdate)
          } yield product
        case _    =>
          new Throwable("The product does not exists or the new product name is occupied.")
            .raiseError[F, Product]
      }

  def getById(orgId: OrgId, productId: ProductId): F[Product] =
    productAlgebra.checkProductExistence(orgId, productId).flatMap {
      case true => productAlgebra.getById(orgId, productId)
      case _    =>
        new Throwable("The product does not exists or the new product name is occupied.")
          .raiseError[F, Product]
    }

  def deleteById(orgId: OrgId, productId: ProductId): F[String] =
    productAlgebra.checkProductExistence(orgId, productId).flatMap {
      case true => productAlgebra.deleteById(orgId, productId)
      case _    => new Throwable("THe product does not exists.").raiseError[F, String]
    }

  def getByParams(
    orgId: OrgId,
    name: Option[String] = None,
    code: Option[String] = None,
    priceFrom: Option[Double] = None,
    priceTo: Option[Double] = None,
    limit: Option[Int] = None,
    offset: Option[Int] = None
  ): F[List[Product]] =
    productAlgebra.getAll(
      name = name,
      code = code,
      priceFrom = priceFrom,
      orgId = orgId,
      priceTo = priceTo,
      limit = limit,
      offset = offset
    )
}

object ProductService {

  private def buildUpdateBody(old: Product, request: ProductUpdate): Product =
    old.copy(
      name = request.name.map(_.toDomain).getOrElse(old.name),
      quantity = request.quantity.map(_.toDomain).getOrElse(old.quantity),
      unitId = request.unitId.getOrElse(old.unitId),
      description = request.description.map(_.toDomain),
      categoryId = request.categoryId.getOrElse(old.categoryId),
      costPrice = request.costPrice.getOrElse(old.costPrice),
      surcharge = request.surcharge.map(_.toDomain).getOrElse(old.surcharge),
      price = request.price.getOrElse(old.price),
      imagePath = request.imagePath.map(_.toDomain),
      saleStart = request.saleStart,
      saleEnd = request.saleEnd
    )

}
