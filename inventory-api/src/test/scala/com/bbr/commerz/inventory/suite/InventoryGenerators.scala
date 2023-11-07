package com.bbr.commerz.inventory.suite

import com.bbr.commerz.auth.domain.auth.AuthPayloads.PasswordUpdate
import com.bbr.commerz.inventory.domain.category.CategoryPayloads._
import com.bbr.commerz.inventory.domain.product.ProductPayloads._
import com.bbr.commerz.inventory.domain.unit.UnitPayloads._
import com.bbr.commerz.organization.suite.OrgGenerators
import com.bbr.platform.domain.Product.ProductId
import eu.timepit.refined.api.Refined
import org.scalacheck.Gen
import squants.market.{Money, USD}

trait InventoryGenerators extends OrgGenerators {

  def genMoney: Gen[Money] = Gen.posNum[Long].map(n => USD(BigDecimal(n)))

  def genUnitId: Gen[UnitId]         = Gen.uuid.map(UnitId.apply)
  def genProductId: Gen[ProductId]   = Gen.uuid.map(ProductId.apply)
  def genCategoryId: Gen[CategoryId] = Gen.uuid.map(CategoryId.apply)

  def genCategoryNameParam: Gen[CategoryNameParam] = genRefinedNEString.map(CategoryNameParam.apply)
  def genCategoryName: Gen[CategoryName]           = genString(5).map(CategoryName.apply)

  def genCategoryRequest: Gen[CategoryRequest] = genCategoryNameParam.map(CategoryRequest.apply)

  def genCategory: Gen[Category] =
    for {
      id    <- genCategoryId
      orgId <- genOrgId
      name  <- genCategoryName
    } yield Category(
      id = id,
      orgId = orgId,
      name = name,
      status = CategoryStatus.ACTIVE
    )

  def genProductDescriptionParam: Gen[ProductDescriptionParam] = genRefinedNEString.map(ProductDescriptionParam.apply)

  def genProductDescription: Gen[ProductDescription] = genRefinedNEString.map(v => ProductDescription.apply(v.value))

  def genQuantity: Gen[Quantity]           = Gen.posNum[Int].map(Quantity.apply)
  def genQuantityParam: Gen[QuantityParam] = Gen.posNum[Int].map(v => QuantityParam.apply(Refined.unsafeApply(v)))

  def genCostPrice: Gen[CostPrice] = genMoney.map(CostPrice.apply)
  def genPrice: Gen[Price]         = genMoney.map(Price.apply)

  def genProductName: Gen[ProductName] = genRefinedNEString.map(v => ProductName.apply(v.value))
  def genProductCode: Gen[ProductCode] = genRefinedNEString.map(v => ProductCode.apply(v.value))
  def genBarCode: Gen[BarCode]         = genRefinedNEString.map(v => BarCode.apply(v.value))
  def genQrCode: Gen[QRCode]           = genRefinedNEString.map(v => QRCode.apply(v.value))

  def genProductNameParam: Gen[ProductNameParam] = genRefinedNEString.map(ProductNameParam.apply)
  def genProductCodeParam: Gen[ProductCodeParam] = genRefinedNEString.map(ProductCodeParam.apply)
  def genBarCodeParam: Gen[BarCodeParam]         = genRefinedNEString.map(BarCodeParam.apply)
  def genQrCodeParam: Gen[QRCodeParam]           = genRefinedNEString.map(QRCodeParam.apply)

  def genImagePathParam: Gen[ImagePathParam] = genURL.map(v => ImagePathParam.apply(Refined.unsafeApply(v)))
  def genImagePath: Gen[ImagePath]           = genURL.map(ImagePath.apply)

  def genSurchargeParam: Gen[SurchargeParam] =
    Gen.choose(0.0, 100.0).map(v => SurchargeParam.apply(Refined.unsafeApply(v)))

  def genSurcharge: Gen[Surcharge] = Gen.choose(0.0, 100.0).map(Surcharge.apply)

  def genProductRequest(unitId: UnitId, categoryId: CategoryId): Gen[ProductRequest] =
    for {
      name                <- genProductNameParam
      quantity            <- genQuantityParam
      description         <- Gen.option(genProductDescriptionParam)
      costPrice           <- genCostPrice
      surcharge           <- genSurchargeParam
      price               <- genPrice
      productCode         <- genProductCodeParam
      saleStart           <- Gen.option(genLocalDateTime)
      saleEnd             <- Gen.option(genLocalDateTime)
      barCode             <- Gen.option(genBarCodeParam)
      qrCode              <- Gen.option(genQrCodeParam)
      expirationDate      <- Gen.option(genLocalDate)
      lastTransactionTime <- Gen.option(genLocalDateTime)
      imagePath           <- Gen.option(genImagePathParam)
    } yield ProductRequest(
      name = name,
      quantity = quantity,
      unitId = unitId,
      description = description,
      categoryId = categoryId,
      costPrice = costPrice,
      surcharge = surcharge,
      price = price,
      productCode = productCode,
      saleStart = saleStart,
      saleEnd = saleEnd,
      barCode = barCode,
      qrCode = qrCode,
      expirationDate = expirationDate,
      lastTransactionTime = lastTransactionTime,
      imagePath = imagePath
    )

  def genProductUpdate(
    unitId: Option[UnitId] = None,
    categoryId: Option[CategoryId] = None
  ): Gen[ProductUpdate] =
    for {
      name        <- Gen.option(genProductNameParam)
      quantity    <- Gen.option(genQuantityParam)
      description <- Gen.option(genProductDescriptionParam)
      costPrice   <- Gen.option(genCostPrice)
      surcharge   <- Gen.option(genSurchargeParam)
      price       <- Gen.option(genPrice)
      imagePath   <- Gen.option(genImagePathParam)
      saleStart   <- Gen.option(genLocalDateTime)
      saleEnd     <- Gen.option(genLocalDateTime)
    } yield ProductUpdate(
      name = name,
      quantity = quantity,
      unitId = unitId,
      description = description,
      categoryId = categoryId,
      costPrice = costPrice,
      surcharge = surcharge,
      price = price,
      imagePath = imagePath,
      saleStart = saleStart,
      saleEnd = saleEnd
    )

  def genProduct: Gen[Product] =
    for {
      id                  <- genProductId
      orgId               <- genOrgId
      branchId            <- Gen.option(genBranchId)
      name                <- genProductName
      quantity            <- genQuantity
      unitId              <- genUnitId
      description         <- Gen.option(genProductDescription)
      categoryId          <- genCategoryId
      costPrice           <- genCostPrice
      surcharge           <- genSurcharge
      price               <- genPrice
      productCode         <- genProductCode
      saleStart           <- Gen.option(genLocalDateTime)
      saleEnd             <- Gen.option(genLocalDateTime)
      barCode             <- Gen.option(genBarCode)
      qrCode              <- Gen.option(genQrCode)
      expirationDate      <- Gen.option(genLocalDate)
      lastTransactionTime <- Gen.option(genLocalDateTime)
      imagePath           <- Gen.option(genImagePath)
      status              <- Gen.oneOf(ProductStatus.values)
      createdAt           <- Gen.option(genLocalDateTime)
      updatedAt           <- Gen.option(genLocalDateTime)
    } yield Product(
      id = id,
      orgId = orgId,
      branchId = branchId,
      name = name,
      quantity = quantity,
      unitId = unitId,
      description = description,
      categoryId = categoryId,
      costPrice = costPrice,
      surcharge = surcharge,
      price = price,
      productCode = productCode,
      saleStart = saleStart,
      saleEnd = saleEnd,
      barCode = barCode,
      qrCode = qrCode,
      expirationDate = expirationDate,
      lastTransactionTime = lastTransactionTime,
      imagePath = imagePath,
      status = status,
      createdAt = createdAt,
      updatedAt = updatedAt
    )

  def genUnitNameParam: Gen[UnitNameParam] = genRefinedNEString.map(UnitNameParam.apply)
  def genUnitName: Gen[UnitName]           = genString(5).map(UnitName.apply)

  def genUnitRequest: Gen[UnitRequest] = genUnitNameParam.map(UnitRequest.apply)

  def genProductUnit: Gen[ProductUnit] =
    for {
      id     <- genUnitId
      orgId  <- genOrgId
      name   <- genUnitName
      status <- Gen.oneOf(UnitStatus.values)
    } yield ProductUnit(id, orgId, name, status)

  def genProductUnits: Gen[List[ProductUnit]] = Gen.listOfN(3, genProductUnit)
  def genProducts: Gen[List[Product]]         = Gen.listOfN(3, genProduct)
  def genCategories: Gen[List[Category]]      = Gen.listOfN(3, genCategory)

  def genPasswordUpdate: Gen[PasswordUpdate] =
    for {
      oldPass <- genPasswordParam
      newPass <- genPasswordParam
    } yield PasswordUpdate(oldPass, newPass)
}
