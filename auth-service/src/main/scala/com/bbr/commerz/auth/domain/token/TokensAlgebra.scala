package com.bbr.commerz.auth.domain.token

import com.bbr.platform.domain.Staff.StaffAuth
import dev.profunktor.auth.jwt._

trait TokensAlgebra[F[_]] {
  def create(user: StaffAuth): F[JwtToken]
}
