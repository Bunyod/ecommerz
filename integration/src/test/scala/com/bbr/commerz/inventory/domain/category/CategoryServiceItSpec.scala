package com.bbr.commerz.inventory.domain.category

import cats.effect.IO
import com.bbr.commerz.inventory.domain.category.CategoryPayloads._
import com.bbr.commerz.inventory.suite.json._
import com.bbr.commerz.organization.domain.owner.OwnerPayloads.Owner
import com.bbr.commerz.utils.ItUtils._
import com.bbr.commerz.utils.ConfigOverrideChecks
import org.scalatest.EitherValues

trait CategoryServiceItSpec extends ConfigOverrideChecks with EitherValues {

  private val service: CategoryService[IO] = services.category
  private val owner: Owner                 = createOwner(genPhoneNumber.sample.get)

  fakeTest(classOf[CategoryServiceItSpec])

  test("create category [OK]") {
    forall(genCategoryRequest) { request =>
      for {
        orgId       <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        created     <- service.create(orgId, request)
        verification =
          expect.all(
            created.name.value == request.name.value.value,
            created.status == CategoryStatus.ACTIVE
          )
      } yield verification
    }
  }

  test("create category [FAILURE]") {
    forall(genCategoryRequest) { request =>
      for {
        orgId       <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        _           <- service.create(orgId, request)
        created2    <- service.create(orgId, request).attempt
        verification =
          expect.all(
            created2.isLeft,
            created2.left.value.getMessage.contains("already exists")
          )
      } yield verification
    }
  }

  test("create and update category [OK]") {
    forall(genCategoryRequest) { request =>
      for {
        orgId        <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        id           <- service.create(orgId, request).map(_.id)
        updateRequest = CategoryRequest(genCategoryNameParam.sample.get)
        updated      <- service.updateById(orgId, id, updateRequest)
        verification  =
          expect.all(updated.id == id, updated.status == CategoryStatus.ACTIVE)
      } yield verification
    }
  }

  test("create and update category [FAILURE]") {
    forall(genCategoryRequest) { request =>
      for {
        orgId        <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        _            <- service.create(orgId, request).map(_.id)
        randomId      = genCategoryId.sample.get
        updateRequest = CategoryRequest(genCategoryNameParam.sample.get)
        updated      <- service.updateById(orgId, randomId, updateRequest).attempt
        verification  =
          expect.all(
            updated.isLeft,
            updated.left.value.getMessage == s"Could not find category with this ID: ${randomId.value}"
          )
      } yield verification
    }
  }

  test("create and get category by id [OK]") {
    forall(genCategoryRequest) { request =>
      for {
        orgId       <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        id          <- service.create(orgId, request).map(_.id)
        getCategory <- service.getById(orgId, id)
        verification =
          expect.all(
            getCategory.id == id,
            getCategory.name.value == request.name.value.value,
            getCategory.status == CategoryStatus.ACTIVE
          )
      } yield verification
    }
  }

  test("create and get category by id [FAILURE]") {
    forall(genCategoryId) { fakeId =>
      for {
        orgId       <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        obtained    <- service.getById(orgId, fakeId).attempt
        verification =
          expect.all(
            obtained.isLeft,
            obtained.left.value.getMessage.contains("Could not find category with this ID")
          )
      } yield verification
    }
  }

  test("create and get categories [OK]") {
    forall(genCategoryRequest) { request =>
      for {
        orgId            <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        category         <- service.create(orgId, request)
        obtainedCategory <- service.getAll(orgId, Some(category.name.value))
        allCategories    <- service.getAll(orgId)
        verifications     =
          expect.all(obtainedCategory.length == 1, allCategories.nonEmpty)
      } yield verifications
    }
  }

  test("create and delete category [OK]") {
    forall(genCategoryRequest) { request =>
      for {
        orgId        <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        id           <- service.create(orgId, request).map(_.id)
        obtained     <- service.getById(orgId, id)
        deleted      <- service.deleteById(orgId, id)
        verifications =
          expect.all(
            obtained.id == id,
            obtained.name.value == request.name.value.value,
            deleted.equals("Successfully deleted")
          )
      } yield verifications
    }
  }

  test("create and delete category [FAILURE]") {
    forall(genCategoryRequest) { request =>
      for {
        orgId        <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        _            <- service.create(orgId, request)
        deleted      <- service.deleteById(orgId, genCategoryId.sample.get).attempt
        verifications = expect.all(
                          deleted.isLeft,
                          deleted.left.value.getMessage.contains("Could not find category with this ID")
                        )
      } yield verifications
    }
  }

  test("delete and get category[FAILURE]") {
    forall(genCategoryRequest) { request =>
      for {
        orgId        <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        created      <- service.create(orgId, request)
        _            <- service.deleteById(orgId, created.id)
        obtained     <- service.getById(orgId, created.id).attempt
        verifications = expect.all(
                          obtained.isLeft,
                          obtained.left.value.getMessage.contains("Could not find category with this ID")
                        )
      } yield verifications
    }
  }

}
