package com.bbr.commerz.inventory.http.product

import cats.effect.IO
import cats.implicits._
import com.bbr.commerz.inventory.domain.category.CategoryPayloads.CategoryId
import com.bbr.commerz.inventory.domain.image.product.{ProductImageAlgebra, ProductImageService}
import com.bbr.commerz.inventory.domain.product.ProductPayloads._
import com.bbr.commerz.inventory.domain.product.{ProductAlgebra, ProductService}
import com.bbr.commerz.inventory.domain.unit.UnitPayloads.UnitId
import com.bbr.commerz.inventory.suite.InventoryGenerators
import com.bbr.commerz.inventory.suite.json._
import com.bbr.commerz.organization.suite.HttpTestSuite
import com.bbr.commerz.inventory.http._
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Product.ProductId
import io.circe.syntax._
import org.http4s.Method._
import org.http4s.client.dsl.io._
import org.http4s.headers.`Content-Type`
import org.http4s.{HttpRoutes, MediaType, Status, Uri}

import java.io.InputStream
import java.util.UUID

object ProductRoutesSpec extends HttpTestSuite with InventoryGenerators {

  val productImageService = new ProductImageService[IO](new TestProductImageRepository, new TestProductRepository)

  def getProducts(products: List[Product]): ProductService[IO] = new ProductService[IO](
    new TestProductRepository {
      override def getAll(
        orgId: OrgId,
        categoryId: Option[CategoryId],
        priceFrom: Option[Double],
        priceTo: Option[Double],
        name: Option[String],
        code: Option[String],
        limit: Option[Int],
        offset: Option[Int]
      ): IO[List[Product]] = IO.pure(products)
    }
  )

  def failingGetProducts(products: List[Product]): ProductService[IO] = new ProductService[IO](
    new TestProductRepository {
      override def getAll(
        orgId: OrgId,
        categoryId: Option[CategoryId],
        priceFrom: Option[Double],
        priceTo: Option[Double],
        name: Option[String],
        code: Option[String],
        limit: Option[Int],
        offset: Option[Int]
      ): IO[List[Product]] =
        IO.raiseError(DummyError) *> IO.pure(products)
    }
  )

  def getProductById(product: Product): ProductService[IO] = new ProductService[IO](
    new TestProductRepository {
      override def getById(orgId: OrgId, id: ProductId): IO[Product] = IO.pure(product)
    }
  )
  def failingGetProductById: ProductService[IO]            = new ProductService[IO](
    new TestProductRepository {
      override def getById(orgId: OrgId, id: ProductId): IO[Product] = IO.raiseError(DummyError)
    }
  )

  def createProduct(newProduct: Product): ProductService[IO] = new ProductService[IO](
    new TestProductRepository {
      override def create(product: Product): IO[Product] =
        IO.pure(newProduct)
      override def checkProductName(
        orgId: OrgId,
        categoryId: CategoryId,
        unitId: UnitId,
        productName: ProductName
      ): IO[Boolean] = false.pure[IO]
    }
  )
  def failingCreateProduct: ProductService[IO]               = new ProductService[IO](
    new TestProductRepository {
      override def create(product: Product): IO[Product] = IO.raiseError(DummyError) *> IO.pure(product)

      override def checkProductName(
        orgId: OrgId,
        categoryId: CategoryId,
        unitId: UnitId,
        productName: ProductName
      ): IO[Boolean] =
        false.pure[IO]
    }
  )

  def deleteProduct(): ProductService[IO]      = new ProductService[IO](
    new TestProductRepository {
      override def deleteById(orgId: OrgId, productId: ProductId): IO[String] = IO.pure("Deleted")
    }
  )
  def failingDeleteProduct: ProductService[IO] = new ProductService[IO](
    new TestProductRepository {
      override def deleteById(orgId: OrgId, id: ProductId): IO[String] =
        IO.raiseError(DummyError) *> IO.pure("Deleted")
    }
  )

  def updateProduct(newProduct: Product): ProductService[IO]     = new ProductService[IO](
    new TestProductRepository {
      override def updateById(id: ProductId, product: Product): IO[Product] = IO.pure(newProduct)
      override def getById(orgId: OrgId, productId: ProductId): IO[Product] = IO.pure(newProduct)
    }
  )
  def failingUpdateProduct(product: Product): ProductService[IO] = new ProductService[IO](
    new TestProductRepository {
      override def updateById(id: ProductId, updateProduct: Product): IO[Product] =
        IO.raiseError(DummyError) *> IO.pure(product)
    }
  )

  test("Get product by ID [OK]") {
    forall(genProduct) { product =>
      val req = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/products/${UUID.randomUUID()}"))

      val routes: HttpRoutes[IO] =
        new ProductRoutes[IO](getProductById(product), productImageService).routes(authMiddleware(staffAuthWithAgent))
      expectHttpStatus(routes, req)(Status.Ok)
    }
  }
  test("Get product by ID [FAILURE]") {
    forall(genOrgId) { orgId =>
      val req = GET(Uri.unsafeFromString(s"/org/${orgId.value}/inventory/products/${UUID.randomUUID()}"))

      val routes: HttpRoutes[IO] =
        new ProductRoutes[IO](failingGetProductById, productImageService) routes (authMiddleware(staffAuthWithAgent))
      expectHttpFailure(routes, req)
    }
  }
  test("Get product by ID bad request [FAILURE]") {
    forall(genOrgId) { id =>
      val req                    = GET(Uri.unsafeFromString(s"/wrong123/inventory/products/${id.value}"))
      val routes: HttpRoutes[IO] =
        new ProductRoutes[IO](failingGetProductById, productImageService) routes (authMiddleware(staffAuthWithAgent))
      expectHttpFailure(routes, req)
    }
  }

  test("GET products [OK]") {
    forall(genProducts) { products =>
      val req                    =
        GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/products"))
      val routes: HttpRoutes[IO] =
        new ProductRoutes[IO](getProducts(products), productImageService) routes (authMiddleware(staffAuthWithAgent))
      expectHttpBodyAndStatus(routes, req)(products, Status.Ok)
    }
  }
  test("GET products [FAILURE]") {
    forall(genProducts) { products =>
      val req                    = GET(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/products"))
      val routes: HttpRoutes[IO] =
        new ProductRoutes[IO](failingGetProducts(products), productImageService) routes (authMiddleware(
          staffAuthWithAgent
        ))
      expectHttpFailure(routes, req)
    }
  }
  test("GET products bad request[FAILURE]") {
    forall(genProducts) { products =>
      val req                    = GET(Uri.unsafeFromString(s"/inventory/org/wrong123/products"))
      val routes: HttpRoutes[IO] =
        new ProductRoutes[IO](failingGetProducts(products), productImageService) routes (authMiddleware(
          staffAuthWithAgent
        ))
      expectHttpFailure(routes, req)
    }
  }

  test("POST add product [OK]") {
    forall(genProduct) { product =>
      val productRequest         = genProductRequest(product.unitId, product.categoryId).sample.get
      val req                    = POST(
        Uri.unsafeFromString(s"/org/${product.orgId.value}/inventory/products")
      )
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(productRequest.asJson)
      val routes: HttpRoutes[IO] =
        new ProductRoutes[IO](createProduct(product), productImageService).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpBodyAndStatus(routes, req)(product, Status.Created)
    }
  }
  test("POST add product [FAILURE]") {
    forall(genProductRequest(genUnitId.sample.get, genCategoryId.sample.get)) { request =>
      val req                    = POST(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/products"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(request.asJson)
      val routes: HttpRoutes[IO] =
        new ProductRoutes[IO](failingCreateProduct, productImageService).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpFailure(routes, req)
    }
  }
  test("POST add product wrong JSON [FAILURE]") {
    forall(genOrgId) { orgId =>
      val req                    = POST(Uri.unsafeFromString(s"/org/${orgId.value}/inventory/products"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(s"WRONG_JSON: $orgId")
      val routes: HttpRoutes[IO] =
        new ProductRoutes[IO](failingCreateProduct, productImageService).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpFailure(routes, req)
    }
  }
  test("POST add product bad request[FAILURE]") {
    forall(genProduct) { product =>
      val req                    = POST(Uri.unsafeFromString(s"wrong123/inventory/products"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(product.asJson)
      val routes: HttpRoutes[IO] =
        new ProductRoutes[IO](failingCreateProduct, productImageService).routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpFailure(routes, req)
    }
  }

  test("PUT update product [OK]") {
    forall(genProduct) { product =>
      val productUpdate          = genProductUpdate(product.unitId.some, product.categoryId.some).sample.get
      val req                    = PUT(
        Uri.unsafeFromString(
          s"/org/${product.orgId.value}/inventory/products/${product.id.value}"
        )
      )
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(productUpdate.asJson)
      val routes: HttpRoutes[IO] =
        new ProductRoutes[IO](updateProduct(product), productImageService).routes(
          authMiddleware(
            staffAuthWithoutAgent
          )
        )
      expectHttpBodyAndStatus(routes, req)(product, Status.Ok)
    }
  }
  test("PUT update product [FAILURE]") {
    forall(genProduct) { product =>
      val productUpdate          = genProductUpdate(product.unitId.some, product.categoryId.some).sample.get
      val id                     = UUID.randomUUID()
      val req                    = PUT(Uri.unsafeFromString(s"/org/${UUID.randomUUID()}/inventory/products/$id"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(productUpdate.asJson)
      val routes: HttpRoutes[IO] =
        new ProductRoutes[IO](failingUpdateProduct(product), productImageService)
          .routes(authMiddleware(staffAuthWithoutAgent))
      expectHttpFailure(routes, req)
    }
  }
  test("PUT update product wrong JSON [FAILURE]") {
    forall(genProduct) { product =>
      val req                    =
        PUT(Uri.unsafeFromString(s"/org/${product.orgId.value}/inventory/products/${product.id.value}"))
          .withContentType(`Content-Type`(MediaType.application.json))
          .withEntity(s"WRONG_JSON: $product")
      val routes: HttpRoutes[IO] =
        new ProductRoutes[IO](failingUpdateProduct(product), productImageService).routes(
          authMiddleware(
            staffAuthWithoutAgent
          )
        )
      expectHttpFailure(routes, req)
    }
  }
  test("PUT update product bad request [FAILURE]") {
    forall(genProduct) { product =>
      val req = PUT(Uri.unsafeFromString(s"wrong123/inventory/products/${UUID.randomUUID()}"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(product.asJson)

      val routes: HttpRoutes[IO] =
        new ProductRoutes[IO](failingUpdateProduct(product), productImageService).routes(
          authMiddleware(
            staffAuthWithoutAgent
          )
        )
      expectHttpFailure(routes, req)
    }
  }

  test("DELETE product [OK]") {
    forall(genOrgId) { orgId =>
      val req                    = DELETE(
        Uri.unsafeFromString(s"/org/${orgId.value}/inventory/products/${UUID.randomUUID()}")
      )
      val routes: HttpRoutes[IO] = new ProductRoutes[IO](deleteProduct(), productImageService).routes(
        authMiddleware(
          staffAuthWithoutAgent
        )
      )
      expectHttpStatus(routes, req)(Status.Ok)
    }
  }
  test("DELETE product [FAILURE]") {
    forall(genOrgId) { orgId =>
      val req                    = DELETE(
        Uri.unsafeFromString(s"/org/${orgId.value}/inventory/products/${UUID.randomUUID()}")
      )
      val routes: HttpRoutes[IO] =
        new ProductRoutes[IO](failingDeleteProduct, productImageService).routes(
          authMiddleware(
            staffAuthWithoutAgent
          )
        )
      expectHttpFailure(routes, req)
    }
  }
  test("DELETE product bad request [FAILURE]") {
    forall(genOrgId) { id =>
      val req                    = DELETE(Uri.unsafeFromString(s"/wrong123/inventory/products/${id.value}"))
      val routes: HttpRoutes[IO] =
        new ProductRoutes[IO](failingDeleteProduct, productImageService).routes(
          authMiddleware(
            staffAuthWithoutAgent
          )
        )
      expectHttpFailure(routes, req)
    }
  }

}

class TestProductRepository extends ProductAlgebra[IO] {
  override def create(product: Product): IO[Product] = IO.pure(product)
  override def getAll(
    orgId: OrgId,
    categoryId: Option[CategoryId],
    priceFrom: Option[Double],
    priceTo: Option[Double],
    name: Option[String],
    code: Option[String],
    limit: Option[Int],
    offset: Option[Int]
  ): IO[List[Product]] = IO.pure(List.empty[Product])

  override def updateById(productId: ProductId, product: Product): IO[Product] = IO.pure(null)

  override def getById(orgId: OrgId, productId: ProductId): IO[Product] = IO.pure(null)

  override def deleteById(orgId: OrgId, productId: ProductId): IO[String] = IO.pure(productId.toString)

  override def updateProductImage(id: UUID, profileImagePath: String): IO[ProductId] =
    IO.pure(ProductId(id))

  override def getProductImagePath(productId: ProductId): IO[String] = IO.pure("image")

  override def checkProductExistence(orgId: OrgId, productId: ProductId): IO[Boolean] = true.pure[IO]

  override def checkProductName(
    orgId: OrgId,
    categoryId: CategoryId,
    unitId: UnitId,
    productName: ProductName
  ): IO[Boolean] =
    true.pure[IO]

  override def decreaseProductQuantity(productId: ProductId, quantity: Quantity): IO[Unit] = IO.unit

  override def increaseProductQuantity(productId: ProductId, quantity: Quantity): IO[Unit] = IO.unit
}

class TestProductImageRepository extends ProductImageAlgebra[IO] {
  override def createBucket(bucketName: String): IO[Unit] = IO.unit

  override def putS3Object(filename: String, is: InputStream, contentType: String): IO[String] = IO.pure("image")

  override def getObjectFile(filePath: String, fileExtension: String): IO[Array[Byte]] = IO.pure(Array(filePath.toByte))
}
