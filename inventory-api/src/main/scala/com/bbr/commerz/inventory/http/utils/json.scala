package com.bbr.commerz.inventory.http.utils

import cats._
import com.bbr.commerz.inventory.domain.category.CategoryPayloads._
import com.bbr.commerz.inventory.domain.product.ProductPayloads._
import com.bbr.commerz.inventory.domain.unit.UnitPayloads._
import com.bbr.commerz.organization.domain.organization.OrganizationPayloads.URL
import com.bbr.platform.domain.Branch.BranchId
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Product.ProductId
import eu.timepit.refined.types.numeric.{PosDouble, PosInt}
import eu.timepit.refined.types.string.NonEmptyString
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.generic.semiauto._
import io.circe.refined._
import org.http4s.EntityEncoder
import org.http4s.circe._
import squants.market.{Money, USD}

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

object json extends JsonCodecs {
  implicit def deriveEntityEncoder[F[_], A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
}

trait JsonCodecs {

  // ----- Coercible codecs -----
  implicit def coercibleEncoder[A, B: Encoder]: Encoder[A] =
    Encoder[B].contramap[A](_.asInstanceOf[B])

  implicit def coercibleKeyEncoder[A, B: KeyEncoder]: KeyEncoder[A] =
    KeyEncoder[B].contramap[A](_.asInstanceOf[B])

  implicit val moneyDecoder: Decoder[Money] =
    Decoder[BigDecimal].map(USD.apply)

  implicit val moneyEncoder: Encoder[Money] =
    Encoder[BigDecimal].contramap(_.amount)

  implicit val showProduct: Show[Product] = Show.show(product => product.name.value)

  implicit val categoryNameDecoder: Decoder[CategoryName] = deriveDecoder[CategoryName]
  implicit val categoryNameEncoder: Encoder[CategoryName] = deriveEncoder[CategoryName]

  implicit val categoryNameParamDecoder: Decoder[CategoryNameParam] =
    Decoder.forProduct1("name")(CategoryNameParam.apply)

  implicit val categoryRequestDecoder: Decoder[CategoryRequest] = Decoder.instance { h =>
    for {
      categoryName <- h.get[NonEmptyString]("category_name").map(CategoryNameParam.apply)
    } yield CategoryRequest(categoryName)
  }

  implicit val categoryRequestEncoder: Encoder[CategoryRequest] = Encoder.forProduct1(
    "category_name"
  )(cr => cr.name.value.value)

  implicit val customConfig: Configuration =
    Configuration.default.withDefaults.withSnakeCaseMemberNames.withSnakeCaseConstructorNames

  implicit val categoryStatusEncoder: Encoder[CategoryStatus] = deriveConfiguredEncoder[CategoryStatus]
  implicit val categoryStatusDecoder: Decoder[CategoryStatus] = deriveConfiguredDecoder[CategoryStatus]

  implicit val categoryEncoder: Encoder[Category] = Encoder.forProduct4(
    "id",
    "org_id",
    "name",
    "status"
  )(c => (c.id.value, c.orgId.value, c.name.value, c.status.entryName))

  implicit val categoryDecoder: Decoder[Category] = Decoder.instance { h =>
    for {
      categoryId   <- h.get[UUID]("category_id").map(CategoryId.apply)
      orgId        <- h.get[UUID]("org_id").map(OrgId.apply)
      categoryName <- h.get[String]("name").map(CategoryName.apply)
      role         <- h.get[String]("status").map(CategoryStatus.withName)
    } yield Category(categoryId, orgId, categoryName, role)
  }

  implicit val productNameParamDecoder: Decoder[ProductNameParam] =
    Decoder.forProduct1("name")(ProductNameParam.apply)

  implicit val productNameParamEncoder: Encoder[ProductNameParam] =
    Encoder.forProduct1("name")(_.value)

  implicit val productNameDecoder: Decoder[ProductName] = Decoder.forProduct1("name")(ProductName.apply)
  implicit val productNameEncoder: Encoder[ProductName] = Encoder.forProduct1("name")(_.value)

  implicit val productCodeDecoder: Decoder[ProductCode] = Decoder.forProduct1("product_code")(ProductCode.apply)
  implicit val productCodeEncoder: Encoder[ProductCode] = Encoder.forProduct1("product_code")(_.value)

  implicit val productCodeParamDecoder: Decoder[ProductCodeParam] =
    Decoder.forProduct1("product_code")(ProductCodeParam.apply)
  implicit val productCodeParamEncoder: Encoder[ProductCodeParam] =
    Encoder.forProduct1("product_code")(_.value)

  implicit val productDescEncoder: Encoder[ProductDescription] =
    Encoder.forProduct1("description")(_.value)

  implicit val productDescDecoder: Decoder[ProductDescription] =
    Decoder.forProduct1("description")(ProductDescription.apply)

  implicit val productDescParamEncoder: Encoder[ProductDescriptionParam] =
    Encoder.forProduct1("description")(_.value)

  implicit val productDescParamDecoder: Decoder[ProductDescriptionParam] =
    Decoder.forProduct1("description")(ProductDescriptionParam.apply)

  implicit val costPriceEncoder: Encoder[CostPrice] = Encoder.forProduct1("cost_price")(_.value)
  implicit val costPriceDecoder: Decoder[CostPrice] = Decoder.forProduct1("cost_price")(CostPrice.apply)

  implicit val barCodeEncoder: Encoder[BarCode] = Encoder.forProduct1("bar_code")(_.value)
  implicit val barCodeDecoder: Decoder[BarCode] = Decoder.forProduct1("bar_code")(BarCode.apply)

  implicit val barCodeParamEncoder: Encoder[BarCodeParam] = Encoder.forProduct1("bar_code")(_.value)
  implicit val barCodeParamDecoder: Decoder[BarCodeParam] = Decoder.forProduct1("bar_code")(BarCodeParam.apply)

  implicit val qrCodeEncoder: Encoder[QRCode] = Encoder.forProduct1("qr_code")(_.value)
  implicit val qrCodeDecoder: Decoder[QRCode] = Decoder.forProduct1("qr_code")(QRCode.apply)

  implicit val qrCodeParamEncoder: Encoder[QRCodeParam] = Encoder.forProduct1("qr_code")(_.value)
  implicit val qrCodeParamDecoder: Decoder[QRCodeParam] = Decoder.forProduct1("qr_code")(QRCodeParam.apply)

  implicit val quantityEncoder: Encoder[Quantity] = Encoder.forProduct1("quantity")(_.value)
  implicit val quantityDecoder: Decoder[Quantity] = Decoder.forProduct1("quantity")(Quantity.apply)

  implicit val quantityParamEncoder: Encoder[QuantityParam] = Encoder.forProduct1("quantity")(_.value)
  implicit val quantityParamDecoder: Decoder[QuantityParam] = Decoder.forProduct1("quantity")(QuantityParam.apply)

  implicit val surchargeParamEncoder: Encoder[SurchargeParam] = Encoder.forProduct1("surcharge")(_.value)
  implicit val surchargeParamDecoder: Decoder[SurchargeParam] = Decoder.forProduct1("surcharge")(SurchargeParam.apply)

  implicit val surchargeEncoder: Encoder[Surcharge] = Encoder.forProduct1("surcharge")(_.value)
  implicit val surchargeDecoder: Decoder[Surcharge] = Decoder.forProduct1("surcharge")(Surcharge.apply)

  implicit val imagePathParamEncoder: Encoder[ImagePathParam] = Encoder.forProduct1("image_path")(_.value)
  implicit val imagePathParamDecoder: Decoder[ImagePathParam] = Decoder.forProduct1("image_path")(ImagePathParam.apply)

  implicit val imagePathEncoder: Encoder[ImagePath] = Encoder.forProduct1("image_path")(_.value)
  implicit val imagePathDecoder: Decoder[ImagePath] = Decoder.forProduct1("image_path")(ImagePath.apply)

  implicit val quantityShow: Show[Quantity] = Show.fromToString

  implicit val productRequestEncoder: Encoder[ProductRequest] = Encoder.forProduct16(
    "name",
    "quantity",
    "unit_id",
    "description",
    "category_id",
    "cost_price",
    "surcharge",
    "price",
    "product_code",
    "sale_start",
    "sale_end",
    "bar_code",
    "qr_code",
    "expiration_date",
    "last_transaction_time",
    "image_path"
  )(p =>
    (
      p.name.value.value,
      p.quantity.value,
      p.unitId.value,
      p.description.map(_.value.value),
      p.categoryId.value,
      p.costPrice.value.amount,
      p.surcharge.value.value,
      p.price.value.amount,
      p.productCode.value.value,
      p.saleStart,
      p.saleEnd,
      p.barCode.map(_.value.value),
      p.qrCode.map(_.value.value),
      p.expirationDate,
      p.lastTransactionTime,
      p.imagePath.map(_.value)
    )
  )

  implicit val productRequestDecoder: Decoder[ProductRequest] = Decoder.instance { h =>
    for {
      name                <- h.get[NonEmptyString]("name").map(ProductNameParam.apply)
      quantity            <- h.get[PosInt]("quantity").map(QuantityParam.apply)
      unitId              <- h.get[UUID]("unit_id").map(UnitId.apply)
      desc                <- h.downField("description").as[Option[NonEmptyString]].map(_.map(ProductDescriptionParam.apply))
      categoryId          <- h.get[UUID]("category_id").map(CategoryId.apply)
      costPrice           <- h.get[BigDecimal]("cost_price").map(m => CostPrice(Money(m, USD)))
      surcharge           <- h.get[PosDouble]("surcharge").map(SurchargeParam.apply)
      price               <- h.get[BigDecimal]("price").map(m => Price(Money(m, USD)))
      productCode         <- h.get[NonEmptyString]("product_code").map(ProductCodeParam.apply)
      saleStart           <- h.downField("sale_start").as[Option[LocalDateTime]]
      saleEnd             <- h.downField("sale_end").as[Option[LocalDateTime]]
      barCode             <- h.downField("bar_code").as[Option[NonEmptyString]].map(_.map(BarCodeParam.apply))
      qrCode              <- h.downField("qr_code").as[Option[NonEmptyString]].map(_.map(QRCodeParam.apply))
      expirationDate      <- h.downField("expiration_date").as[Option[LocalDate]]
      lastTransactionTime <- h.downField("sale_start").as[Option[LocalDateTime]]
      imagePath           <- h.downField("image_path").as[Option[URL]].map(_.map(ImagePathParam.apply))
    } yield ProductRequest(
      name = name,
      quantity = quantity,
      unitId = unitId,
      description = desc,
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
  }

  implicit val productEncoder: Encoder[Product] = Encoder.forProduct22(
    "id",
    "org_id",
    "branch_id",
    "name",
    "quantity",
    "unit_id",
    "description",
    "category_id",
    "cost_price",
    "surcharge",
    "price",
    "product_code",
    "sale_start",
    "sale_end",
    "bar_code",
    "qr_code",
    "expiration_date",
    "last_transaction_time",
    "image_path",
    "status",
    "created_at",
    "updated_at"
  )(p =>
    (
      p.id.value,
      p.orgId.value,
      p.branchId.map(_.value),
      p.name.value,
      p.quantity.value,
      p.unitId.value,
      p.description.map(_.value),
      p.categoryId.value,
      p.costPrice.value.amount,
      p.surcharge.value,
      p.price.value.amount,
      p.productCode.value,
      p.saleStart,
      p.saleEnd,
      p.barCode.map(_.value),
      p.qrCode.map(_.value),
      p.expirationDate,
      p.lastTransactionTime,
      p.imagePath.map(_.value),
      p.status.entryName,
      p.createdAt,
      p.updatedAt
    )
  )

  implicit val productDecoder: Decoder[Product] = Decoder.instance { h =>
    for {
      id                  <- h.get[UUID]("id").map(ProductId.apply)
      orgId               <- h.get[UUID]("org_id").map(OrgId.apply)
      branchId            <- h.downField("branch_id").as[Option[UUID]].map(_.map(BranchId.apply))
      name                <- h.get[String]("name").map(ProductName.apply)
      quantity            <- h.get[Int]("quantity").map(Quantity.apply)
      unitId              <- h.get[UUID]("unit_id").map(UnitId.apply)
      desc                <- h.downField("description").as[Option[String]].map(_.map(ProductDescription.apply))
      categoryId          <- h.get[UUID]("category_id").map(CategoryId.apply)
      costPrice           <- h.get[BigDecimal]("cost_price").map(m => CostPrice(Money(m, USD)))
      surcharge           <- h.get[Double]("surcharge").map(Surcharge.apply)
      price               <- h.get[BigDecimal]("price").map(m => Price(Money(m, USD)))
      productCode         <- h.get[String]("product_code").map(ProductCode.apply)
      saleStart           <- h.downField("sale_start").as[Option[LocalDateTime]]
      saleEnd             <- h.downField("sale_end").as[Option[LocalDateTime]]
      barCode             <- h.downField("bar_code").as[Option[String]].map(_.map(BarCode.apply))
      qrCode              <- h.downField("qr_code").as[Option[String]].map(_.map(QRCode.apply))
      expirationDate      <- h.downField("expiration_date").as[Option[LocalDate]]
      lastTransactionTime <- h.downField("sale_start").as[Option[LocalDateTime]]
      imagePath           <- h.downField("image_path").as[Option[String]].map(_.map(ImagePath.apply))
      status              <- h.get[String]("status").map(s => ProductStatus.withName(s))
      createdAt           <- h.downField("created_at").as[Option[LocalDateTime]]
      updatedAt           <- h.downField("updated_at").as[Option[LocalDateTime]]
    } yield Product(
      id = id,
      orgId = orgId,
      branchId = branchId,
      name = name,
      quantity = quantity,
      unitId = unitId,
      description = desc,
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
  }

  implicit val priceEncoder: Encoder[Price] = deriveEncoder[Price]
  implicit val priceDecoder: Decoder[Price] = deriveDecoder[Price]

  implicit val productStatusEncoder: Encoder[ProductStatus] = deriveConfiguredEncoder[ProductStatus]
  implicit val productStatusDecoder: Decoder[ProductStatus] = deriveConfiguredDecoder[ProductStatus]

  implicit val unitNameEncoder: Encoder[UnitName] = deriveEncoder[UnitName]
  implicit val unitNameDecoder: Decoder[UnitName] = deriveDecoder[UnitName]

  implicit val unitNameParamEncoder: Encoder[UnitNameParam] = Encoder.forProduct1("unit_name")(_.value)
  implicit val unitNameParamDecoder: Decoder[UnitNameParam] = Decoder.forProduct1("unit_name")(UnitNameParam.apply)

  implicit val unitStatusDecoder: Decoder[UnitStatus] = deriveConfiguredDecoder[UnitStatus]
  implicit val unitStatusEncoder: Encoder[UnitStatus] = deriveConfiguredEncoder[UnitStatus]

  implicit val unitRequestEncoder: Encoder[UnitRequest] = Encoder.forProduct1(
    "unit_name"
  )(u => u.name.value.value)

  implicit val unitRequestDecoder: Decoder[UnitRequest] = Decoder.instance { h =>
    for {
      unitName <- h.get[NonEmptyString]("unit_name").map(UnitNameParam.apply)
    } yield UnitRequest(unitName)
  }

  implicit val productUnitEncoder: Encoder[ProductUnit] = Encoder.forProduct4(
    "id",
    "org_id",
    "unit_name",
    "status"
  )(u => (u.id.value, u.orgId.value, u.name.value, u.status.entryName))

  implicit val productUnitDecoder: Decoder[ProductUnit] = Decoder.instance { h =>
    for {
      unitId   <- h.get[UUID]("unit_id").map(UnitId.apply)
      orgId    <- h.get[UUID]("org_id").map(OrgId.apply)
      unitName <- h.get[String]("unit_name").map(UnitName.apply)
      role     <- h.get[String]("status").map(UnitStatus.withName)
    } yield ProductUnit(unitId, orgId, unitName, role)
  }

  implicit val unitReqEncoder: Encoder[UnitRequest] = Encoder.forProduct1("name")(u => u.name)

  implicit val productUpdateEncoder: Encoder[ProductUpdate] = Encoder.forProduct11(
    "name",
    "quantity",
    "unit_id",
    "description",
    "category_id",
    "cost_price",
    "surcharge",
    "price",
    "image_path",
    "sale_start",
    "sale_end"
  )(p =>
    (
      p.name.map(_.value),
      p.quantity.map(_.value),
      p.unitId.map(_.value),
      p.description.map(_.value),
      p.categoryId.map(_.value),
      p.costPrice.map(_.value.amount),
      p.surcharge.map(_.value),
      p.price.map(_.value.amount),
      p.imagePath.map(_.value),
      p.saleStart,
      p.saleEnd
    )
  )

  implicit val productUpdateDecoder: Decoder[ProductUpdate] = Decoder.instance { h =>
    for {
      name       <- h.downField("name").as[Option[NonEmptyString]].map(_.map(ProductNameParam.apply))
      quantity   <- h.downField("quantity").as[Option[PosInt]].map(_.map(QuantityParam.apply))
      unitId     <- h.downField("unit_id").as[Option[UUID]].map(_.map(UnitId.apply))
      desc       <- h.downField("description").as[Option[NonEmptyString]].map(_.map(ProductDescriptionParam.apply))
      categoryId <- h.downField("category_id").as[Option[UUID]].map(_.map(CategoryId.apply))
      costPrice  <- h.downField("cost_price").as[Option[BigDecimal]].map(_.map(m => CostPrice(Money(m, USD))))
      surcharge  <- h.downField("surcharge").as[Option[PosDouble]].map(_.map(SurchargeParam.apply))
      price      <- h.downField("price").as[Option[BigDecimal]].map(_.map(m => Price(Money(m, USD))))
      imagePath  <- h.downField("image_path").as[Option[URL]].map(_.map(ImagePathParam.apply))
      saleStart  <- h.downField("sale_start").as[Option[LocalDateTime]]
      saleEnd    <- h.downField("sale_end").as[Option[LocalDateTime]]
    } yield ProductUpdate(
      name = name,
      quantity = quantity,
      unitId = unitId,
      description = desc,
      categoryId = categoryId,
      costPrice = costPrice,
      surcharge = surcharge,
      price = price,
      imagePath = imagePath,
      saleStart = saleStart,
      saleEnd = saleEnd
    )
  }
}
