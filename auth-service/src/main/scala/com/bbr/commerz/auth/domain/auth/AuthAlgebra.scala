package com.bbr.commerz.auth.domain.auth

import com.bbr.commerz.auth.domain.auth.AuthPayloads.RecoveryData
import com.bbr.commerz.organization.domain.organization.OrganizationPayloads.OrganizationName
import com.bbr.commerz.organization.domain.owner.OwnerPayloads.Owner
import com.bbr.commerz.organization.domain.staff.StaffPayloads.Staff
import com.bbr.platform.domain.Staff.{EncryptedPassword, PhoneNumber, StaffId, StaffRole}
import dev.profunktor.auth.jwt.JwtToken

trait AuthAlgebra[F[_]] {
  def login(phoneNumber: PhoneNumber, password: EncryptedPassword): F[JwtToken]
  def logout(token: JwtToken, phoneNumber: PhoneNumber): F[Unit]
  def getPassword(staffId: StaffId, role: StaffRole): F[EncryptedPassword]
  def updatePassword(staffId: StaffId, role: StaffRole, password: EncryptedPassword): F[Unit]
  def getOwner(phoneNumber: PhoneNumber): F[Option[Owner]]
  def getStaff(phoneNumber: PhoneNumber, orgName: OrganizationName): F[Option[Staff]]
  def saveRecoveryCode(phoneNumber: PhoneNumber, code: Int): F[Unit]
  def getRecoveryData(phoneNumber: PhoneNumber): F[RecoveryData]
  def deleteRecoveryCode(phoneNumber: PhoneNumber): F[Unit]
}
