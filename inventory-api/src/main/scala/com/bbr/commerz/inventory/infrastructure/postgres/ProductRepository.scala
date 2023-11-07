package com.bbr.commerz.inventory.infrastructure.postgres

import cats.effect._
import cats.implicits._
import com.bbr.commerz.inventory.domain.product._
import com.bbr.commerz.inventory.domain.product.ProductPayloads._
import com.bbr.commerz.inventory.domain.category.CategoryPayloads.CategoryId
import com.bbr.commerz.inventory.domain.unit.UnitPayloads.UnitId
import ProductRepository._
import com.bbr.platform.domain.Branch.BranchId
import com.bbr.platform.{getCurrentTime, TimeOpts, UuidOpts}
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Product.ProductId
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._ // DON'T REMOVE IT
import doobie.implicits.javasql._  // DON'T REMOVE IT
import doobie.util.transactor.Transactor
import doobie.util.update.Update0
import squants.market.{Money, USD}

import java.sql.Timestamp
import java.util.UUID

class ProductRepository[F[_]: Async](val tr: Transactor[F]) extends ProductAlgebra[F] {

  override def create(product: Product): F[Product] =
    insert(product).run.transact(tr) *> getById(product.orgId, product.id)

  override def updateById(productId: ProductId, product: Product): F[Product] =
    update(productId.value, product).run.transact(tr) *> getById(product.orgId, productId)

  override def getById(orgId: OrgId, productId: ProductId): F[Product] =
    selectById(orgId.value, productId.value).option.transact(tr).flatMap {
      case Some(product) => product.pure[F]
      case None          => new Throwable(s"Product not found with ID: $productId").raiseError[F, Product]
    }

  override def deleteById(orgId: OrgId, productId: ProductId): F[String] =
    delete(orgId.value, productId.value).run.transact(tr) *> "Successfully deleted".pure[F]

  override def getAll(
    orgId: OrgId,
    categoryId: Option[CategoryId] = None,
    priceFrom: Option[Double] = None,
    priceTo: Option[Double] = None,
    name: Option[String] = None,
    code: Option[String] = None,
    limit: Option[Int],
    offset: Option[Int]
  ): F[List[Product]] =
    selectAll(
      orgId = orgId.value,
      categoryId = categoryId.map(_.value),
      priceFrom = priceFrom,
      priceTo = priceTo,
      name = name,
      code = code,
      limit = limit,
      offset = offset
    ).to[List].transact(tr)

  override def updateProductImage(id: UUID, profileImagePath: String): F[ProductId] =
    updateProductImagePath(id, profileImagePath).run.transact(tr) *> ProductId(id).pure[F]

  override def getProductImagePath(productId: ProductId): F[String] =
    getImagePath(productId.value).unique.transact(tr)

  override def checkProductExistence(orgId: OrgId, productId: ProductId): F[Boolean] =
    checkProduct(orgId.value, productId.value).unique.transact(tr)

  override def checkProductName(
    orgId: OrgId,
    categoryId: CategoryId,
    unitId: UnitId,
    productName: ProductName
  ): F[Boolean] =
    checkProductNameExistence(orgId.value, categoryId.value, unitId.value, productName.value).unique.transact(tr)

  override def decreaseProductQuantity(productId: ProductId, quantity: Quantity): F[Unit] =
    decreaseQuantity(productId.value, quantity.value).run.transact(tr).void

  override def increaseProductQuantity(productId: ProductId, quantity: Quantity): F[Unit] =
    increaseQuantity(productId.value, quantity.value).run.transact(tr).void

}

object ProductRepository {

  private def decreaseQuantity(productId: UUID, quantity: Int): Update0 =
    sql"""
         UPDATE PRODUCT SET
         QUANTITY = QUANTITY - $quantity
         WHERE ID = $productId
       """.update

  private def increaseQuantity(productId: UUID, quantity: Int): Update0 =
    sql"""
         UPDATE PRODUCT SET
         QUANTITY = QUANTITY + $quantity
         WHERE ID = $productId
       """.update

  private def checkProduct(orgId: UUID, productId: UUID): Query0[Boolean] =
    sql"""SELECT EXISTS(SELECT * FROM PRODUCT WHERE (
      ID = $productId AND
      STATUS = ${ProductStatus.ACTIVE.entryName} AND
      ORG_ID = $orgId
    ))""".query[Boolean]

  private def checkProductNameExistence(
    orgId: UUID,
    categoryId: UUID,
    unitId: UUID,
    productName: String
  ): Query0[Boolean] =
    sql"""SELECT EXISTS(SELECT * FROM PRODUCT WHERE (
      CATEGORY_ID = $categoryId AND
      UNIT_ID = $unitId AND
      NAME = $productName AND
      ORG_ID = $orgId
    ))""".query[Boolean]

  private def getImagePath(productId: UUID): Query0[String] =
    sql"""SELECT IMAGE_PATH FROM PRODUCT WHERE ID = $productId""".query[String]

  private def updateProductImagePath(id: UUID, profileImagePath: String): Update0 =
    sql"""UPDATE PRODUCT SET IMAGE_PATH = $profileImagePath WHERE ID = $id""".update

  private def insert(product: Product): Update0 =
    sql"""INSERT INTO PRODUCT (
          ID,
          ORG_ID,
          BRANCH_ID,
          NAME,
          QUANTITY,
          UNIT_ID,
          DESCRIPTION,
          CATEGORY_ID,
          COST_PRICE,
          SURCHARGE,
          PRICE,
          PRODUCT_CODE,
          SALE_START,
          SALE_END,
          BAR_CODE,
          QR_CODE,
          EXPIRATION_DATE,
          LAST_TRANSACTION_TIME,
          IMAGE_PATH,
          STATUS,
          CREATED_AT,
          UPDATED_AT
          ) VALUES (
          ${product.id.value},
          ${product.orgId.value},
          ${product.branchId.map(_.value)},
          ${product.name.value},
          ${product.quantity.value},
          ${product.unitId.value},
          ${product.description.map(_.value)},
          ${product.categoryId.value},
          ${product.costPrice.value.amount},
          ${product.surcharge},
          ${product.price.value.amount},
          ${product.productCode.value},
          ${product.saleStart.map(ss => Timestamp.valueOf(ss))},
          ${product.saleEnd.map(se => Timestamp.valueOf(se))},
          ${product.barCode.map(_.value)},
          ${product.qrCode.map(_.value)},
          ${product.expirationDate.map(ed => java.sql.Date.valueOf(ed))},
          ${product.lastTransactionTime.map(lt => Timestamp.valueOf(lt))},
          ${product.imagePath.map(_.value)},
          ${product.status.entryName},
          ${product.createdAt.map(ca => Timestamp.valueOf(ca))},
          ${product.updatedAt.map(ua => Timestamp.valueOf(ua))}
          )""".update

  private def update(
    id: UUID,
    product: Product
  ): Update0 =
    sql"""
         UPDATE PRODUCT SET
         NAME = ${product.name.value},
         QUANTITY = ${product.quantity.value},
         UNIT_ID = ${product.unitId.value},
         DESCRIPTION = ${product.description.map(_.value)},
         CATEGORY_ID = ${product.categoryId.value},
         COST_PRICE = ${product.costPrice.value.amount},
         SURCHARGE = ${product.surcharge},
         PRICE = ${product.price.value.amount},
         IMAGE_PATH = ${product.imagePath.map(_.value)},
         SALE_START = ${product.saleStart.map(ss => Timestamp.valueOf(ss))},
         SALE_END = ${product.saleEnd.map(se => Timestamp.valueOf(se))},
         UPDATED_AT = ${Timestamp.valueOf(getCurrentTime)}
         WHERE ID = $id
       """.update

  private def selectById(orgId: UUID, productId: UUID): Query0[Product] =
    sql"""
         SELECT * FROM PRODUCT WHERE ID = $productId AND ORG_ID = $orgId
       """.query

  private def delete(orgId: UUID, productId: UUID): Update0 =
    sql"""
          UPDATE PRODUCT SET
          STATUS = ${ProductStatus.INACTIVE.entryName},
          UPDATED_AT = ${getCurrentTime.some}
          WHERE ID = $productId AND
          ORG_ID = $orgId
          """.update

  private def selectAll(
    orgId: UUID,
    categoryId: Option[UUID],
    priceFrom: Option[Double],
    priceTo: Option[Double],
    name: Option[String],
    code: Option[String],
    limit: Option[Int],
    offset: Option[Int]
  ): Query0[Product] =
    sql"""
          SELECT * FROM PRODUCT
          WHERE
          ORG_ID = $orgId
          AND
          ($categoryId IS NULL OR CATEGORY_ID = $categoryId)
          AND
          ($name IS NULL OR NAME = $name)
          AND
          ($code IS NULL OR PRODUCT_CODE = $code)
          AND
          ($priceFrom IS NULL OR PRICE >= $priceFrom)
          AND
          ($priceTo IS NULL OR PRICE <= $priceTo)
          ORDER BY NAME
          OFFSET COALESCE($offset, 0) ROWS
          FETCH NEXT COALESCE($limit, 50) ROWS ONLY;
          """.query

  implicit val productRead: Read[Product] =
    Read[
      (
        UUID,
        UUID,
        Option[UUID],
        String,
        Int,
        UUID,
        Option[String],
        UUID,
        BigDecimal,
        Double,
        BigDecimal,
        String,
        Option[Timestamp],
        Option[Timestamp],
        Option[String],
        Option[String],
        Option[Timestamp],
        Option[Timestamp],
        Option[String],
        String,
        Option[Timestamp],
        Option[Timestamp]
      )
    ].map {
      case (
            id,
            orgId,
            branchId,
            name,
            quantity,
            unitId,
            description,
            categoryId,
            costPrice,
            surcharge,
            price,
            productCode,
            saleStart,
            saleEnd,
            barCode,
            qrCode,
            expirationDate,
            lastTransactionTime,
            imagePath,
            status,
            createdAt,
            updatedAt
          ) =>
        Product(
          ProductId(id),
          orgId.toOrgId,
          branchId.map(BranchId.apply),
          ProductName(name),
          Quantity(quantity),
          UnitId(unitId),
          description.map(ProductDescription.apply),
          CategoryId(categoryId),
          CostPrice(Money(costPrice, USD)),
          Surcharge(surcharge),
          Price(Money(price, USD)),
          ProductCode(productCode),
          saleStart.map(_.toLocalDateTime.truncateTime),
          saleEnd.map(_.toLocalDateTime.truncateTime),
          barCode.map(BarCode.apply),
          qrCode.map(QRCode.apply),
          expirationDate.map(_.toLocalDateTime.toLocalDate),
          lastTransactionTime.map(_.toLocalDateTime.truncateTime),
          imagePath.map(ImagePath.apply),
          ProductStatus.withName(status),
          createdAt.map(_.toLocalDateTime.truncateTime),
          updatedAt.map(_.toLocalDateTime.truncateTime)
        )
    }

  implicit val productWrite: Write[Product] =
    Write[
      (
        UUID,
        UUID,
        Option[UUID],
        String,
        Int,
        UUID,
        Option[String],
        UUID,
        BigDecimal,
        Double,
        BigDecimal,
        String,
        Option[Timestamp],
        Option[Timestamp],
        Option[String],
        Option[String],
        Option[Timestamp],
        Option[Timestamp],
        Option[String],
        String,
        Option[Timestamp],
        Option[Timestamp]
      )
    ].contramap { product =>
      (
        product.id.value,
        product.orgId.value,
        product.branchId.map(_.value),
        product.name.value,
        product.quantity.value,
        product.unitId.value,
        product.description.map(_.value),
        product.categoryId.value,
        product.costPrice.value.amount,
        product.surcharge.value,
        product.price.value.amount,
        product.productCode.value,
        product.saleStart.map(t => Timestamp.valueOf(t)),
        product.saleEnd.map(t => Timestamp.valueOf(t)),
        product.barCode.map(_.value),
        product.qrCode.map(_.value),
        product.expirationDate.map(t => Timestamp.valueOf(t.atStartOfDay)),
        product.lastTransactionTime.map(t => Timestamp.valueOf(t)),
        product.imagePath.map(_.value),
        product.status.entryName,
        product.createdAt.map(t => Timestamp.valueOf(t)),
        product.updatedAt.map(t => Timestamp.valueOf(t))
      )
    }
}
