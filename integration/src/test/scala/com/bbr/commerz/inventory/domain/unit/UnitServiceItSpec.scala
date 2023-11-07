package com.bbr.commerz.inventory.domain.unit

import cats.effect.IO
import cats.implicits._
import com.bbr.commerz.inventory.domain.unit.UnitPayloads.{UnitRequest, UnitStatus}
import com.bbr.commerz.inventory.suite.json._
import com.bbr.commerz.organization.domain.owner.OwnerPayloads.Owner
import com.bbr.commerz.utils.ConfigOverrideChecks
import com.bbr.commerz.utils.ItUtils.{createOwner, services}
import org.scalatest.EitherValues

trait UnitServiceItSpec extends ConfigOverrideChecks with EitherValues {

  private val service: UnitService[IO] = services.unit
  private val owner: Owner             = createOwner(genPhoneNumber.sample.get)

  fakeTest(classOf[UnitServiceItSpec])

  test("create unit [OK]") {
    forall(genUnitRequest) { request =>
      for {
        orgId       <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        created     <- service.create(orgId, request)
        verification =
          expect.all(
            created.name.value == request.name.value.value,
            created.status == UnitStatus.ACTIVE
          )
      } yield verification
    }
  }

  test("create unit [FAILURE]") {
    forall(genUnitRequest) { request =>
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

  test("create and update unit [OK]") {
    forall(genUnitRequest) { request =>
      for {
        orgId        <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        id           <- service.create(orgId, request).map(_.id)
        updateRequest = UnitRequest(genUnitNameParam.sample.get)
        updated      <- service.updateById(orgId, id, updateRequest)
        verification  =
          expect.all(updated.id == id, updated.status == UnitStatus.ACTIVE)
      } yield verification
    }
  }

  test("create and update unit [FAILURE]") {
    forall(genUnitRequest) { request =>
      for {
        orgId        <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        _            <- service.create(orgId, request).map(_.id)
        updateRequest = UnitRequest(genUnitNameParam.sample.get)
        updated      <- service.updateById(orgId, genUnitId.sample.get, updateRequest).attempt
        verification  =
          expect.all(
            updated.isLeft,
            updated.left.value.getMessage.contains("Could not find unit with this ID")
          )
      } yield verification
    }
  }

  test("create and get unit by id [OK]") {
    forall(genUnitRequest) { request =>
      for {
        orgId       <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        id          <- service.create(orgId, request).map(_.id)
        productUnit <- service.getById(orgId, id)
        verification =
          expect.all(
            productUnit.id == id,
            productUnit.name.value == request.name.value.value,
            productUnit.status == UnitStatus.ACTIVE
          )
      } yield verification
    }
  }

  test("create and get unit by id [FAILURE]") {
    forall(genUnitId) { unitId =>
      for {
        orgId       <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        obtained    <- service.getById(orgId, unitId).attempt
        verification =
          expect.all(
            obtained.isLeft,
            obtained.left.value.getMessage.contains("Could not find unit with this ID")
          )
      } yield verification
    }
  }

  test("create and get categories [OK]") {
    forall(genUnitRequest) { request =>
      for {
        orgId            <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        unit             <- service.create(orgId, request)
        obtainedCategory <- service.getAll(orgId, unit.name.value.some)
        allCategories    <- service.getAll(orgId)
        verifications     =
          expect.all(obtainedCategory.length == 1, allCategories.nonEmpty)
      } yield verifications
    }
  }

  test("create and delete unit [OK]") {
    forall(genUnitRequest) { request =>
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

  test("create and delete unit [FAILURE]") {
    forall(genUnitRequest) { request =>
      for {
        orgId        <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        _            <- service.create(orgId, request)
        deleted      <- service.deleteById(orgId, genUnitId.sample.get).attempt
        verifications = expect.all(
                          deleted.isLeft,
                          deleted.left.value.getMessage.contains("Could not find unit with this ID")
                        )
      } yield verifications
    }
  }

  test("delete and get unit [FAILURE]") {
    forall(genUnitRequest) { request =>
      for {
        orgId        <- services.organization.create(genOrganizationRequest.sample.get, owner.phoneNumber).map(_.id)
        created      <- service.create(orgId, request)
        _            <- service.deleteById(orgId, created.id)
        obtained     <- service.getById(orgId, created.id).attempt
        verifications = expect.all(
                          obtained.isLeft,
                          obtained.left.value.getMessage.contains("Could not find unit with this ID")
                        )
      } yield verifications
    }
  }
}
