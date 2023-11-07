package com.bbr.commerz.inventory.domain.product

import cats.effect._
import cats.implicits._
import com.bbr.commerz.inventory.domain.category.CategoryService
import com.bbr.commerz.inventory.domain.product.ProductPayloads._
import com.bbr.commerz.inventory.domain.unit.UnitService
import com.bbr.commerz.organization.suite.json._
import com.bbr.commerz.organization.domain.organization.OrganizationService
import com.bbr.commerz.organization.domain.owner.OwnerPayloads.Owner
import com.bbr.commerz.utils.ConfigOverrideChecks
import com.bbr.commerz.utils.ItUtils._

trait ProductServiceItSpec extends ConfigOverrideChecks {

  private val service: ProductService[IO]          = services.product
  private val orgService: OrganizationService[IO]  = services.organization
  private val unitService: UnitService[IO]         = services.unit
  private val categoryService: CategoryService[IO] = services.category

  private val owner: Owner = createOwner(genPhoneNumber.sample.get)

  fakeTest(classOf[ProductServiceItSpec])

  test("create product [OK]") {
    forall(genOrganizationRequest) { orgRequest =>
      for {
        orgId          <- orgService.create(orgRequest, owner.phoneNumber).map(_.id)
        unitRequest     = genUnitRequest.sample.get
        unitId         <- unitService.create(orgId, unitRequest).map(_.id)
        categoryRequest = genCategoryRequest.sample.get
        categoryId     <- categoryService.create(orgId, categoryRequest).map(_.id)
        productRequest  = genProductRequest(unitId, categoryId).sample.get
        created        <- service.create(orgId, branchId = None, productRequest)
        verification    = expect.all(
                            created.name.value == productRequest.name.value.value,
                            created.unitId == productRequest.unitId
                          )
      } yield verification
    }
  }

  test("create product [FAILURE]") {
    forall(genOrganizationRequest) { orgRequest =>
      for {
        orgId          <- orgService.create(orgRequest, owner.phoneNumber).map(_.id)
        unitRequest     = genUnitRequest.sample.get
        unitId         <- unitService.create(orgId, unitRequest).map(_.id)
        categoryRequest = genCategoryRequest.sample.get
        categoryId     <- categoryService.create(orgId, categoryRequest).map(_.id)
        productRequest  = genProductRequest(unitId, categoryId).sample.get
        _              <- service.create(orgId, branchId = None, productRequest)
        created2       <- service.create(orgId, branchId = None, productRequest).attempt
        verification    = expect.all(created2.isLeft)
      } yield verification
    }
  }

  test("create and update product [OK]") {
    forall(genOrganizationRequest) { orgRequest =>
      for {
        orgId          <- orgService.create(orgRequest, owner.phoneNumber).map(_.id)
        unitRequest     = genUnitRequest.sample.get
        unitId         <- unitService.create(orgId, unitRequest).map(_.id)
        categoryRequest = genCategoryRequest.sample.get
        categoryId     <- categoryService.create(orgId, categoryRequest).map(_.id)
        productRequest  = genProductRequest(unitId, categoryId).sample.get
        updateRequest   = genProductUpdate(unitId.some, categoryId.some).sample.get
        id             <- service.create(orgId, branchId = None, productRequest).map(_.id)
        updated        <- service.updateById(orgId, id, updateRequest)
        verification    = expect.all(updated.id == id, updated.status == ProductStatus.ACTIVE)
      } yield verification
    }
  }

  test("create and update product [FAILURE]") {
    forall(genOrganizationRequest) { orgRequest =>
      for {
        orgId          <- orgService.create(orgRequest, owner.phoneNumber).map(_.id)
        unitRequest     = genUnitRequest.sample.get
        unitId         <- unitService.create(orgId, unitRequest).map(_.id)
        categoryRequest = genCategoryRequest.sample.get
        categoryId     <- categoryService.create(orgId, categoryRequest).map(_.id)
        request         = genProductRequest(unitId, categoryId).sample.get
        productUpdate   = genProductUpdate(unitId.some, categoryId.some).sample.get
        _              <- service.create(orgId, branchId = None, request).map(_.id)
        updated        <- service.updateById(orgId, genProductId.sample.get, productUpdate).attempt
        verification    = expect.all(updated.isLeft)
      } yield verification
    }
  }

  test("create and get product by id [OK]") {
    forall(genOrganizationRequest) { orgRequest =>
      for {
        orgId          <- orgService.create(orgRequest, owner.phoneNumber).map(_.id)
        unitRequest     = genUnitRequest.sample.get
        unitId         <- unitService.create(orgId, unitRequest).map(_.id)
        categoryRequest = genCategoryRequest.sample.get
        categoryId     <- categoryService.create(orgId, categoryRequest).map(_.id)
        productRequest  = genProductRequest(unitId, categoryId).sample.get
        productId      <- service.create(orgId, branchId = None, productRequest).map(_.id)
        obtained       <- service.getById(orgId, productId)
        verification    = expect.all(obtained.id == productId, obtained.status == ProductStatus.ACTIVE)
      } yield verification
    }
  }

  test("create and get product by id [FAILURE]") {
    forall(genProductId) { productId =>
      for {
        orgId       <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        obtained    <- service.getById(orgId, productId).attempt
        verification = expect.all(obtained.isLeft)
      } yield verification
    }
  }

  test("create and get products [OK]") {
    forall(genOrganizationRequest) { orgRequest =>
      for {
        orgId          <- orgService.create(orgRequest, owner.phoneNumber).map(_.id)
        unitRequest     = genUnitRequest.sample.get
        unitId         <- unitService.create(orgId, unitRequest).map(_.id)
        categoryRequest = genCategoryRequest.sample.get
        categoryId     <- categoryService.create(orgId, categoryRequest).map(_.id)
        productRequest  = genProductRequest(unitId, categoryId).sample.get
        product        <- service.create(orgId, branchId = None, productRequest)
        obtained       <- service.getByParams(
                            orgId = orgId,
                            name = Some(product.name.value),
                            code = Some(product.productCode.value)
                          )
        allProducts    <- service.getByParams(orgId)
        verification    = expect.all(obtained.length == 1, allProducts.nonEmpty)
      } yield verification
    }
  }

  test("create and delete product [OK]") {
    forall(genOrganizationRequest) { orgRequest =>
      for {
        orgId          <- orgService.create(orgRequest, owner.phoneNumber).map(_.id)
        unitRequest     = genUnitRequest.sample.get
        unitId         <- unitService.create(orgId, unitRequest).map(_.id)
        categoryRequest = genCategoryRequest.sample.get
        categoryId     <- categoryService.create(orgId, categoryRequest).map(_.id)
        productRequest  = genProductRequest(unitId, categoryId).sample.get
        id             <- service.create(orgId, branchId = None, productRequest).map(_.id)
        obtained       <- service.getById(orgId, id)
        deleted        <- service.deleteById(orgId, id)
        verification    = expect.all(
                            obtained.id == id,
                            obtained.name.value == productRequest.name.value.value,
                            deleted.equals("Successfully deleted")
                          )
      } yield verification
    }
  }

  test("create and delete product [FAILURE]") {
    forall(genOrganizationRequest) { orgRequest =>
      for {
        orgId          <- orgService.create(orgRequest, owner.phoneNumber).map(_.id)
        unitRequest     = genUnitRequest.sample.get
        unitId         <- unitService.create(orgId, unitRequest).map(_.id)
        categoryRequest = genCategoryRequest.sample.get
        categoryId     <- categoryService.create(orgId, categoryRequest).map(_.id)
        productRequest  = genProductRequest(unitId, categoryId).sample.get
        _              <- service.create(orgId, branchId = None, productRequest)
        deleted        <- service.deleteById(orgId, genProductId.sample.get).attempt
        verification    = expect.all(deleted.isLeft)
      } yield verification
    }
  }
}
