package com.bbr.commerz.organization.domain.organization

import com.bbr.commerz.organization.suite.json._
import com.bbr.commerz.utils.ConfigOverrideChecks
import com.bbr.commerz.utils.ItUtils.{createOwner, services}
import OrganizationPayloads._
import cats.effect.IO
import com.bbr.commerz.organization.domain.owner.OwnerPayloads.Owner
import org.scalatest.EitherValues

trait OrganizationServiceItSpec extends ConfigOverrideChecks with EitherValues {

  private val service: OrganizationService[IO] = services.organization
  private val owner: Owner                     = createOwner(genPhoneNumber.sample.get)

  fakeTest(classOf[OrganizationServiceItSpec])

  test("create organization [OK]") {
    forall(genOrganizationRequest) { request =>
      for {
        created     <- service.create(request, owner.phoneNumber)
        verification = expect.all(
                         created.name == request.name.toDomain,
                         created.status == OrganizationStatus.ACTIVE
                       )
      } yield verification
    }
  }

  test("create organization, existing organization name [FAILURE]") {
    forall(genOrganizationRequest) { request =>
      for {
        _           <- service.create(request, owner.phoneNumber)
        created     <- service.create(OrganizationRequest(request.name), owner.phoneNumber).attempt
        verification = expect.all(
                         created.isLeft,
                         created.left.value.getMessage.contains("already exists")
                       )
      } yield verification
    }
  }

  test("update organization [OK]") {
    forall(genOrganizationRequest) { request =>
      for {
        created     <- service.create(request, owner.phoneNumber)
        updated     <- service.updateById(created.id, genOrganizationRequest.sample.get)
        verification = expect.all(
                         updated.status == OrganizationStatus.ACTIVE,
                         updated.id == created.id,
                         updated.createdAt == created.createdAt
                       )
      } yield verification
    }
  }

  test("update organization, non-existing organization [FAILURE]") {
    forall(genOrganizationRequest) { request =>
      for {
        updated     <- service.updateById(genOrgId.sample.get, request).attempt
        verification = expect.all(
                         updated.isLeft,
                         updated.left.value.getMessage.contains("Organization does not exist")
                       )
      } yield verification
    }
  }

  test("create and get organization by ID [OK]") {
    forall(genOrganizationRequest) { request =>
      for {
        created     <- service.create(request, owner.phoneNumber)
        obtained    <- service.getById(created.id)
        verification = expect.all(
                         obtained.id == created.id,
                         obtained.status == created.status,
                         obtained.updatedAt == created.updatedAt,
                         obtained.createdAt == created.createdAt,
                         obtained.name == created.name
                       )
      } yield verification
    }
  }

  test("create and get organization by ID, non-existing organization [FAILURE]") {
    forall(genOrgId) { orgId =>
      for {
        obtained    <- service.getById(orgId).attempt
        verification = expect.all(
                         obtained.isLeft,
                         obtained.left.value.getMessage.contains("Organization not found with ID")
                       )
      } yield verification
    }
  }

  test("get all organizations [OK]") {
    forall(genOrganizationRequest) { request =>
      for {
        _                <- service.create(request, owner.phoneNumber)
        allOrganizations <- service.getAll(None, None)
        verifications     = expect.all(allOrganizations.nonEmpty)
      } yield verifications
    }
  }

  test("get all organizations with query params [OK]") {
    forall(genOrganizationRequest) { request =>
      for {
        created      <- service.create(request, owner.phoneNumber)
        obtained     <- service.getAll(created.name, None, None)
        verifications =
          expect.all(obtained.length == 1)
      } yield verifications
    }
  }

  test("delete organization by ID [OK]") {
    forall(genOrganizationRequest) { request =>
      for {
        created      <- service.create(request, owner.phoneNumber)
        deleted      <- service.deleteById(created.id)
        verifications = expect.all(
                          deleted.equals("Successfully deleted")
                        )
      } yield verifications
    }
  }

  test("delete organization by ID, non-existing organization [FAILURE]") {
    forall(genOrganizationRequest) { request =>
      for {
        created      <- service.create(request, owner.phoneNumber)
        _            <- service.deleteById(created.id)
        deleted2     <- service.deleteById(created.id).attempt
        verifications = expect.all(
                          deleted2.isLeft,
                          deleted2.left.value.getMessage.contains("Could not find organization with this ID")
                        )
      } yield verifications
    }
  }
}
