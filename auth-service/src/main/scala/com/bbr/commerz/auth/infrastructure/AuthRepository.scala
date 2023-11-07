package com.bbr.commerz.auth.infrastructure

import cats.effect.Async
import cats.implicits._
import com.bbr.commerz.auth.domain.auth.AuthAlgebra
import com.bbr.commerz.auth.domain.auth.AuthPayloads._
import com.bbr.commerz.auth.domain.token.TokensAlgebra
import com.bbr.commerz.auth.infrastructure.Drivers._
import com.bbr.commerz.organization.domain.organization.OrganizationPayloads.{Organization, OrganizationName}
import com.bbr.commerz.organization.domain.owner.OwnerPayloads.Owner
import com.bbr.commerz.organization.domain.staff.StaffPayloads._
import com.bbr.commerz.organization.infrastructure.postgres.Drivers._
import com.bbr.platform.config.Configuration._
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.{EncryptedPassword, PhoneNumber, StaffAuth, StaffId, StaffRole}
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._ // DON'T REMOVE IT
import doobie.util.transactor.Transactor

class AuthRepository[F[_]: Async](
  tokenExpiration: TokenExpiration,
  tokens: TokensAlgebra[F],
  redis: RedisCommands[F, String, String],
  tr: Transactor[F]
) extends AuthAlgebra[F] {

  import AuthRepository._

  private def getStaff(
    phoneNumber: PhoneNumber,
    password: EncryptedPassword
  ): F[Option[Staff]] = selectStaff(phoneNumber, password).option.transact(tr)

  private def getOwner(
    phoneNumber: PhoneNumber,
    password: EncryptedPassword
  ): F[Option[Owner]] = AuthRepository.selectOwner(phoneNumber, password).option.transact(tr)

  override def login(
    phoneNumber: PhoneNumber,
    password: EncryptedPassword
  ): F[JwtToken] =
    getStaff(phoneNumber, password).flatMap {
      case None =>
        getOwner(phoneNumber, password).flatMap {
          case None => InvalidUserOrPassword(phoneNumber.value).raiseError[F, JwtToken]

          case Some(owner) if owner.status == StaffStatus.INACTIVE =>
            new Throwable(s"The staff not found with phone number: ${owner.phoneNumber.value}").raiseError[F, JwtToken]

          case Some(owner) =>
            redis.get(phoneNumber.value).flatMap {
              case Some(t) => JwtToken(t).pure[F]
              case None    =>
                val u = StaffAuth(owner.id, None, phoneNumber, owner.role)
                tokens.create(u).flatTap { t =>
                  redis.setEx(phoneNumber.value, t.value, tokenExpiration.value) *>
                    redis.setEx(t.value, phoneNumber.value, tokenExpiration.value)
                }
            }
        }

      case Some(staff) if staff.status == StaffStatus.INACTIVE =>
        new Throwable(s"The staff not found with phone number: ${staff.phoneNumber.value}").raiseError[F, JwtToken]

      case Some(staff) =>
        redis.get(phoneNumber.value).flatMap {
          case Some(t) => JwtToken(t).pure[F]
          case None    =>
            val u = StaffAuth(staff.id, staff.branchId, phoneNumber, staff.role)
            tokens.create(u).flatTap { t =>
              redis.setEx(phoneNumber.value, t.value, tokenExpiration.value) *>
                redis.setEx(t.value, phoneNumber.value, tokenExpiration.value)
            }
        }
    }

  override def logout(token: JwtToken, phoneNumber: PhoneNumber): F[Unit] =
    redis.del(token.value) *> redis.del(phoneNumber.value).void

  override def getPassword(
    staffId: StaffId,
    role: StaffRole
  ): F[EncryptedPassword] =
    role match {
      case StaffRole.OWNER => selectOwner(staffId).unique.transact(tr).map(_.password)
      case _               => selectStaff(staffId).unique.transact(tr).map(_.password)
    }

  override def updatePassword(
    staffId: StaffId,
    role: StaffRole,
    password: EncryptedPassword
  ): F[Unit] =
    role match {
      case StaffRole.OWNER => setOwnerPassword(staffId, password).run.transact(tr).void
      case _               => setPassword(staffId, password).run.transact(tr).void
    }

  override def getOwner(phoneNumber: PhoneNumber): F[Option[Owner]] =
    selectOwner(phoneNumber).option.transact(tr)

  override def getStaff(phoneNumber: PhoneNumber, orgName: OrganizationName): F[Option[Staff]] =
    (for {
      org   <- getOrganization(orgName).unique
      staff <- selectStaff(phoneNumber, org.id).option
    } yield staff).transact(tr)

  override def saveRecoveryCode(phoneNumber: PhoneNumber, code: Int): F[Unit] =
    selectCode(phoneNumber).option
      .flatMap(
        _.fold(saveCode(phoneNumber, code).run)(_ => removeCode(phoneNumber).run *> saveCode(phoneNumber, code).run)
      )
      .transact(tr)
      .void

  override def getRecoveryData(phoneNumber: PhoneNumber): F[RecoveryData] =
    selectCode(phoneNumber).unique.transact(tr)

  override def deleteRecoveryCode(phoneNumber: PhoneNumber): F[Unit] =
    removeCode(phoneNumber).run.transact(tr).void
}

object AuthRepository {

  private def getOrganization(orgName: OrganizationName): Query0[Organization] =
    sql"SELECT * FROM ORGANIZATION WHERE NAME = ${orgName.name}".query[Organization]

  private def selectStaff(phoneNumber: PhoneNumber, password: EncryptedPassword): Query0[Staff] =
    sql"SELECT * FROM STAFF WHERE PHONE_NUMBER = ${phoneNumber.value} AND PASSWORD = ${password.value}".query[Staff]

  private def selectOwner(phoneNumber: PhoneNumber, password: EncryptedPassword): Query0[Owner] =
    sql"SELECT * FROM OWNER WHERE PHONE_NUMBER = ${phoneNumber.value} AND PASSWORD = ${password.value}".query[Owner]

  private def selectOwner(staffId: StaffId): Query0[Owner] =
    sql"SELECT * FROM OWNER WHERE ID = ${staffId.value}".query[Owner]

  private def selectStaff(staffId: StaffId): Query0[Staff] =
    sql"SELECT * FROM STAFF WHERE ID = ${staffId.value}".query[Staff]

  private def selectOwner(phoneNumber: PhoneNumber): Query0[Owner] =
    sql"SELECT * FROM OWNER WHERE PHONE_NUMBER = ${phoneNumber.value}".query[Owner]

  private def selectStaff(phoneNumber: PhoneNumber, orgId: OrgId): Query0[Staff] =
    sql"SELECT * FROM STAFF WHERE PHONE_NUMBER = ${phoneNumber.value} AND ORG_ID = ${orgId.value}".query[Staff]

  private def setOwnerPassword(staffId: StaffId, password: EncryptedPassword): Update0 =
    sql"UPDATE OWNER SET PASSWORD = ${password.value} WHERE ID = ${staffId.value}".update

  private def setPassword(staffId: StaffId, password: EncryptedPassword): Update0 =
    sql"UPDATE STAFF SET PASSWORD = ${password.value} WHERE ID = ${staffId.value}".update

  private def saveCode(phoneNumber: PhoneNumber, code: Int): Update0 =
    sql"INSERT INTO RECOVERY (PHONE_NUMBER, CODE) VALUES (${phoneNumber.value}, $code)".update

  private def selectCode(phoneNumber: PhoneNumber): Query0[RecoveryData] =
    sql"SELECT * FROM RECOVERY WHERE PHONE_NUMBER = ${phoneNumber.value}".query[RecoveryData]

  private def removeCode(phoneNumber: PhoneNumber): Update0 =
    sql"DELETE FROM RECOVERY WHERE PHONE_NUMBER = ${phoneNumber.value}".update

}
