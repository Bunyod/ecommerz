package com.bbr.commerz.auth.domain.auth

import cats.effect.Async
import cats.implicits._
import com.bbr.commerz.auth.domain.auth.AuthPayloads.{PasswordUpdate, RecoveryData}
import com.bbr.commerz.organization.domain.organization.OrganizationPayloads.OrganizationName
import com.bbr.platform.crypto.CryptoAlgebra
import com.bbr.platform.domain.Staff.{Password, PhoneNumber, StaffId, StaffRole}
import com.bbr.platform.effekts.GenRecoveryCode
import dev.profunktor.auth.jwt.JwtToken

class AuthService[F[_]: Async: GenRecoveryCode](
  repository: AuthAlgebra[F],
  crypto: CryptoAlgebra
) {

  def login(phoneNumber: PhoneNumber, password: Password): F[JwtToken] =
    repository.login(phoneNumber, crypto.encrypt(password))

  def logout(token: JwtToken, phoneNumber: PhoneNumber): F[Unit] =
    repository.logout(token, phoneNumber)

  def updatePassword(staffId: StaffId, role: StaffRole, request: PasswordUpdate): F[Unit] =
    repository.getPassword(staffId, role).flatMap { current =>
      if (current == crypto.encrypt(request.oldPassword.toDomain)) {
        repository.updatePassword(staffId, role, crypto.encrypt(request.newPassword.toDomain))
      } else {
        new Throwable("Invalid current password. Please check and try again.").raiseError[F, Unit]
      }
    }

  def recover(phoneNumber: PhoneNumber, orgName: OrganizationName): F[Unit] =
    for {
      code  <- GenRecoveryCode[F].make
      owner <- repository.getOwner(phoneNumber)
      staff <- repository.getStaff(phoneNumber, orgName)
      _     <- if (owner.isDefined || staff.isDefined) {
                 repository.saveRecoveryCode(phoneNumber, code)
               } else {
                 new Throwable(s"The user not found: ${phoneNumber.value}").raiseError[F, Unit]
               }
    } yield ()

  def verify(recoveryData: RecoveryData): F[Unit] =
    repository.getRecoveryData(recoveryData.phoneNumber).flatMap { data =>
      if (data.recoveryCode == recoveryData.recoveryCode) {
        repository.deleteRecoveryCode(recoveryData.phoneNumber) *> ().pure[F]
      } else {
        new Throwable("Incorrect recovery code. Please check and try again.").raiseError[F, Unit]
      }
    }

  def updatePassword(phoneNumber: PhoneNumber, password: Password, orgName: OrganizationName): F[Unit] =
    repository.getOwner(phoneNumber).flatMap {
      case Some(o) => repository.updatePassword(o.id, o.role, crypto.encrypt(password))
      case None    =>
        repository.getStaff(phoneNumber, orgName).flatMap {
          case Some(s) => repository.updatePassword(s.id, s.role, crypto.encrypt(password))
          case None    =>
            new Throwable(s"Couldn't update password, user not found: ${phoneNumber.value}").raiseError[F, Unit]
        }
    }

}
