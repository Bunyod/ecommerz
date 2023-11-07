package com.bbr.commerz.organization

import cats.data.Kleisli
import cats.effect.IO
import cats.syntax.option._
import com.bbr.platform.UuidOpts
import com.bbr.platform.domain.Staff.{PhoneNumber, StaffAuth, StaffRole}
import org.http4s.server.AuthMiddleware

import java.util.UUID

package object http {

  def authMiddleware(role: StaffRole): AuthMiddleware[IO, StaffAuth] = {
    val auth = StaffAuth(
      id = UUID.randomUUID().toStaffId,
      branchId = UUID.randomUUID().toBranchId.some,
      phoneNumber = PhoneNumber("+995489665544"),
      role = role
    )

    AuthMiddleware(Kleisli.pure(auth))
  }

}
