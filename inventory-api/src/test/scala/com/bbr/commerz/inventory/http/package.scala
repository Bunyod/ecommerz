package com.bbr.commerz.inventory

import cats.data.Kleisli
import cats.effect.IO
import com.bbr.commerz.inventory.http.category.CategoryRoutesSpec.{genStaffAuth, genStaffAuthWithoutAgent}
import com.bbr.platform.domain.Staff.StaffAuth
import org.http4s.server.AuthMiddleware

package object http {
  def staffAuthWithAgent: StaffAuth    = genStaffAuth.sample.get
  def staffAuthWithoutAgent: StaffAuth = genStaffAuthWithoutAgent.sample.get

  def authMiddleware(staffAuth: StaffAuth): AuthMiddleware[IO, StaffAuth] = AuthMiddleware(Kleisli.pure(staffAuth))
}
