package com.bbr.commerz.inventory.domain.product

import cats.implicits._
import enumeratum._
import com.bbr.commerz.inventory.domain.category.CategoryPayloads.CategoryId
import com.bbr.commerz.inventory.domain.unit.UnitPayloads._

import java.time.LocalDate
import com.bbr.commerz.organization.domain.organization.OrganizationPayloads.URL
import com.bbr.platform.domain.Branch.BranchId
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Product.ProductId
import com.bbr.platform.getCurrentTime
import eu.timepit.refined.types.numeric.{PosDouble, PosInt}
import eu.timepit.refined.types.string.NonEmptyString
import squants.market.{Money, USD}

import java.time.LocalDateTime

object ProductPayloads {

  case class ProductCodeParam(value: NonEmptyString) {
    def toDomain: ProductCode = ProductCode(value.value)
  }

  case class ProductNameParam(value: NonEmptyString) {
    def toDomain: ProductName = ProductName(value.value)
  }

  case class ProductDescriptionParam(value: NonEmptyString) {
    def toDomain: ProductDescription = ProductDescription(value.value)
  }

  case class BarCodeParam(value: NonEmptyString) {
    def toDomain: BarCode = BarCode(value.value)
  }

  case class QRCodeParam(value: NonEmptyString) {
    def toDomain: QRCode = QRCode(value.value)
  }

  case class ImagePathParam(value: URL)   {
    def toDomain: ImagePath = ImagePath(value.value)
  }
  case class QuantityParam(value: PosInt) {
    def toDomain: Quantity = Quantity(value.value)
  }

  case class SurchargeParam(value: PosDouble) {
    def toDomain: Surcharge = Surcharge(value.value)
  }

  case class QRCode(value: String)
  case class CostPrice(value: Money)
  case class Price(value: Money)
  case class Quantity(value: Int)
  case class ProductCode(value: String)
  case class BarCode(value: String)
  case class ProductName(value: String)
  case class ProductDescription(value: String)
  case class Surcharge(value: Double)
  case class ImagePath(value: String)

  case class ProductRequest(
    name: ProductNameParam,
    quantity: QuantityParam,
    unitId: UnitId,
    description: Option[ProductDescriptionParam],
    categoryId: CategoryId,
    costPrice: CostPrice,
    surcharge: SurchargeParam,
    price: Price,
    productCode: ProductCodeParam,
    saleStart: Option[LocalDateTime],
    saleEnd: Option[LocalDateTime],
    barCode: Option[BarCodeParam],
    qrCode: Option[QRCodeParam],
    expirationDate: Option[LocalDate],
    lastTransactionTime: Option[LocalDateTime],
    imagePath: Option[ImagePathParam]
  ) {
    def toDomain(
      id: ProductId,
      orgId: OrgId,
      branchId: Option[BranchId] = None
    ): Product =
      Product(
        id = id,
        orgId = orgId,
        branchId = branchId,
        name = name.toDomain,
        quantity = quantity.toDomain,
        unitId = unitId,
        description = description.map(_.toDomain),
        categoryId = categoryId,
        costPrice = costPrice,
        surcharge = surcharge.toDomain,
        price = price,
        productCode = productCode.toDomain,
        saleStart = saleStart,
        saleEnd = saleEnd,
        barCode = barCode.map(_.toDomain),
        qrCode = qrCode.map(_.toDomain),
        expirationDate = expirationDate,
        lastTransactionTime = lastTransactionTime,
        imagePath = imagePath.map(_.toDomain),
        status = ProductStatus.ACTIVE,
        createdAt = getCurrentTime.some,
        updatedAt = getCurrentTime.some
      )
  }

  case class CartProduct(product: Product, quantity: Quantity) {
    def subtotal: Money = USD(product.price.value.amount * quantity.value)
  }

  case class Product(
    id: ProductId,
    orgId: OrgId,
    branchId: Option[BranchId],
    name: ProductName,
    quantity: Quantity,
    unitId: UnitId,
    description: Option[ProductDescription],
    categoryId: CategoryId,
    costPrice: CostPrice,
    surcharge: Surcharge,
    price: Price,
    productCode: ProductCode,
    saleStart: Option[LocalDateTime],
    saleEnd: Option[LocalDateTime],
    barCode: Option[BarCode],
    qrCode: Option[QRCode],
    expirationDate: Option[LocalDate],
    lastTransactionTime: Option[LocalDateTime],
    imagePath: Option[ImagePath],
    status: ProductStatus,
    createdAt: Option[LocalDateTime],
    updatedAt: Option[LocalDateTime]
  ) {
    def cart(q: Quantity): CartProduct = CartProduct(this, q)
  }

  case class ProductUpdate(
    name: Option[ProductNameParam],
    quantity: Option[QuantityParam],
    unitId: Option[UnitId],
    description: Option[ProductDescriptionParam],
    categoryId: Option[CategoryId],
    costPrice: Option[CostPrice],
    surcharge: Option[SurchargeParam],
    price: Option[Price],
    imagePath: Option[ImagePathParam],
    saleStart: Option[LocalDateTime],
    saleEnd: Option[LocalDateTime]
  )

  sealed trait ProductStatus extends EnumEntry
  object ProductStatus       extends Enum[ProductStatus] with CirceEnum[ProductStatus] {
    val values: IndexedSeq[ProductStatus] = findValues
    case object ACTIVE   extends ProductStatus
    case object INACTIVE extends ProductStatus
  }
}
