package com.bbr.commerz.auth.domain.auth

import dev.profunktor.auth.jwt.JwtToken
import pdi.jwt.JwtClaim

class UserAuthService[F[_], A](staffAuthAlgebra: UserAuthAlgebra[F, A]) {

  def findUser(jwtToken: JwtToken)(claim: JwtClaim): F[Option[A]] =
    staffAuthAlgebra.findUser(jwtToken)(claim)

}
