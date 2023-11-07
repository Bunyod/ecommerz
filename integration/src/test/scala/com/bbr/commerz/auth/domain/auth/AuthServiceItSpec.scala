package com.bbr.commerz.auth.domain.auth

import cats.Show
import cats.effect.{IO, Resource}
import cats.effect.unsafe.implicits.global
import cats.implicits._
import com.bbr.commerz.auth.domain.auth.AuthPayloads.{PasswordUpdate, RecoveryData}
import com.bbr.commerz.auth.domain.token.TokensService
import com.bbr.commerz.auth.infrastructure.AuthRepository
import com.bbr.commerz.organization.domain.organization.OrganizationPayloads.Organization
import com.bbr.commerz.organization.domain.organization.OrganizationService
import com.bbr.commerz.organization.domain.owner.OwnerPayloads.Owner
import com.bbr.commerz.organization.domain.staff.StaffService
import com.bbr.commerz.utils.ItUtils._
import com.bbr.commerz.utils.ConfigOverrideChecks
import com.bbr.platform.domain.Staff.{Password, PasswordParam, StaffAuth}
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.NoOp.instance
import eu.timepit.refined.api.Refined

trait AuthServiceItSpec extends ConfigOverrideChecks {

  implicit val showPasswordUpdate: Show[PasswordUpdate] = Show.fromToString

  private val orgService: OrganizationService[IO] = services.organization
  private val tokensService: TokensService[IO]    = new TokensService[IO](config.userJwt, config.tokenExpiration)
  private val staffService: StaffService[IO]      = services.staff
  private val owner: Owner                        = createOwner(genPhoneNumber.sample.get)
  private val org: Organization                   =
    orgService.create(genOrganizationRequest.sample.get, owner.phoneNumber).unsafeRunSync()

  override type Res = AuthService[IO]

  override def sharedResource: Resource[IO, Res] = Redis[IO]
    .utf8(config.redis.uri.value)
    .map { redis =>
      val authRepository = new AuthRepository[IO](
        tokenExpiration = config.tokenExpiration,
        tokens = tokensService,
        redis = redis,
        tr = transactor
      )
      new AuthService[IO](authRepository, cryptoService)
    }

  fakeTest(classOf[AuthServiceItSpec])

  test("login staff [OK]") { service =>
    forall(genStaffRequest) { request =>
      for {
        created     <- staffService.create(org.id, request.copy(branchId = None))
        logged      <- service.login(created.phoneNumber, cryptoService.decrypt(created.password)).attempt
        verification = expect.all(logged.isRight)
      } yield verification
    }
  }

  test("login staff, invalid password [FAILURE]") { service =>
    forall(genStaffRequest) { request =>
      for {
        created     <- staffService.create(org.id, request.copy(branchId = None))
        logged      <- service.login(created.phoneNumber, genPassword.sample.get).attempt
        verification = expect.all(logged.isLeft)
      } yield verification
    }
  }

  test("update staff password [OK]") { service =>
    forall(genStaffRequest) { request =>
      for {
        created  <- staffService.create(org.id, request.copy(branchId = None))
        current   = PasswordParam(Refined.unsafeApply(cryptoService.decrypt(created.password).value))
        passwords = genPasswordUpdate.sample.get.copy(oldPassword = current)
        updated  <- service.updatePassword(created.id, created.role, passwords).attempt
      } yield expect(updated.isRight)
    }
  }

  test("update staff password, invalid current password [FAILURE]") { service =>
    forall(genStaffRequest) { request =>
      for {
        created  <- staffService.create(org.id, request.copy(branchId = None))
        passwords = genPasswordUpdate.sample.get
        updated  <- service.updatePassword(created.id, created.role, passwords).attempt
      } yield expect.all(updated.isLeft, updated.left.value.getMessage.contains("Invalid current password"))
    }
  }

  test("staff password recovery [OK]") { service =>
    forall(genStaffRequest) { request =>
      for {
        staff   <- staffService.create(org.id, request.copy(branchId = None))
        _       <- service.recover(staff.phoneNumber, org.name)
        code    <- AuthServiceItSpec.getCode(staff.phoneNumber.value).map(_.recoveryCode)
        _       <- service.verify(RecoveryData(staff.phoneNumber, code))
        password = Password("new_password_for_staff")
        _       <- service.updatePassword(staff.phoneNumber, password, org.name)
        login   <- service.login(staff.phoneNumber, password).attempt
      } yield expect(login.isRight)
    }
  }

  test("staff password recovery, non-existing user [FAILURE]") { service =>
    forall(()) { _ =>
      service.recover(genPhoneNumber.sample.get, org.name).attempt.map { result =>
        expect.all(result.isLeft, result.left.value.getMessage.contains("The user not found"))
      }
    }
  }

  test("staff password recovery, invalid verification code [FAILURE]") { service =>
    forall(genStaffRequest) { request =>
      for {
        staff  <- staffService.create(org.id, request.copy(branchId = None))
        _      <- service.recover(staff.phoneNumber, org.name)
        result <- service.verify(RecoveryData(staff.phoneNumber, 12345)).attempt
      } yield expect.all(result.isLeft, result.left.value.getMessage.contains("Incorrect recovery code"))
    }
  }

  test("login owner [OK]") { service =>
    forall(()) { _ =>
      for {
        logged      <- service.login(owner.phoneNumber, cryptoService.decrypt(owner.password)).attempt
        verification = expect.all(logged.isRight)
      } yield verification
    }
  }

  test("login owner, invalid password [FAILURE]") { service =>
    forall(()) { _ =>
      for {
        logged      <- service.login(owner.phoneNumber, genPassword.sample.get).attempt
        verification = expect.all(logged.isLeft)
      } yield verification
    }
  }

  test("update owner password [OK]") { service =>
    forall(genPasswordUpdate) { passwords =>
      val current = PasswordParam(Refined.unsafeApply(cryptoService.decrypt(owner.password).value))
      for {
        updated <- service.updatePassword(owner.id, owner.role, passwords.copy(oldPassword = current)).attempt
      } yield expect(updated.isRight)
    }
  }

  test("update owner password, invalid current password [FAILURE]") { service =>
    forall(genPasswordUpdate) { passwords =>
      for {
        updated <- service.updatePassword(owner.id, owner.role, passwords).attempt
      } yield expect.all(updated.isLeft, updated.left.value.getMessage.contains("Invalid current password"))
    }
  }

  test("owner password recovery [OK]") { service =>
    forall(()) { _ =>
      for {
        _       <- service.recover(owner.phoneNumber, org.name)
        code    <- AuthServiceItSpec.getCode(owner.phoneNumber.value).map(_.recoveryCode)
        _       <- service.verify(RecoveryData(owner.phoneNumber, code))
        password = Password("new_password")
        _       <- service.updatePassword(owner.phoneNumber, password, org.name)
        login   <- service.login(owner.phoneNumber, password).attempt
      } yield expect(login.isRight)
    }
  }

  test("LOGOUT [OK]") { service =>
    forall(genStaffRequest) { request =>
      for {
        created     <- staffService.create(org.id, request.copy(branchId = None))
        token       <- tokensService.create(StaffAuth(created.id, None, created.phoneNumber, created.role))
        loggedOut   <- service.logout(JwtToken(token.value), created.phoneNumber)
        verification = expect.all(loggedOut.pure[IO] == ().pure[IO])
      } yield verification
    }
  }

}

object AuthServiceItSpec {

  import doobie.implicits._

  private def getCode(phoneNumber: String): IO[RecoveryData] =
    sql"SELECT * FROM RECOVERY WHERE PHONE_NUMBER = $phoneNumber".query[RecoveryData].unique.transact(transactor)

}
