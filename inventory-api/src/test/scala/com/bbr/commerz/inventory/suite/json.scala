package com.bbr.commerz.inventory.suite

import cats._
import com.bbr.commerz.inventory.domain.category.CategoryPayloads._
import com.bbr.commerz.inventory.domain.product.ProductPayloads._
import com.bbr.commerz.inventory.domain.unit.UnitPayloads.{ProductUnit, UnitId, UnitNameParam, UnitRequest}
import com.bbr.platform.domain.Organization.OrgId
import io.circe._
import io.circe.refined._
import org.http4s.EntityEncoder
import org.http4s.circe._
import squants.market.{Money, USD}

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

  implicit val moneyEncoder: Encoder[Money] =
    Encoder[BigDecimal].contramap(_.amount)

  implicit val moneyDecoder: Decoder[Money] =
    Decoder[BigDecimal].map(USD.apply)

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
      p.quantity.value.value,
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
      p.imagePath.map(_.value.value)
    )
  )

  implicit val categoryRequestEncoder: Encoder[CategoryRequest] = Encoder.forProduct1(
    "category_name"
  )(cr => cr.name.value.value)

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

  implicit val showCategoryId: Show[CategoryId]           = Show.fromToString
  implicit val showCategory: Show[Category]               = Show.fromToString
  implicit val showCategoryRequest: Show[CategoryRequest] = Show.fromToString

  implicit val showProduct: Show[Product]               = Show.fromToString
  implicit val showProductRequest: Show[ProductRequest] = Show.fromToString

  implicit val showUnitId: Show[UnitId]           = Show.fromToString
  implicit val showUnitRequest: Show[UnitRequest] = Show.fromToString

  implicit val showCategoryName: Show[CategoryName] = Show.fromToString

  implicit val unitNameParamEncoder: Encoder[UnitNameParam] =
    Encoder.forProduct1("unit_name")(_.value)

  implicit val productUnitEncoder: Encoder[ProductUnit] = Encoder.forProduct4(
    "id",
    "org_id",
    "unit_name",
    "status"
  )(u => (u.id.value, u.orgId.value, u.name.value, u.status.entryName))

  implicit val unitRequestEncoder: Encoder[UnitRequest] = Encoder.forProduct1(
    "unit_name"
  )(u => u.name.value.value)

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
      p.name.map(_.value.value),
      p.quantity.map(_.value),
      p.unitId.map(_.value),
      p.description.map(_.value),
      p.categoryId.map(_.value),
      p.costPrice.map(_.value.amount),
      p.surcharge.map(_.value.value),
      p.price.map(_.value.amount),
      p.imagePath.map(_.value.value),
      p.saleStart,
      p.saleEnd
    )
  )

}
