package com.bbr.commerz.auth.domain.token

import cats.effect.Sync
import cats.implicits._
import com.bbr.commerz.organization.http.utils.json.staffAuthEncoder
import com.bbr.platform.config.Configuration._
import com.bbr.platform.domain.Staff.StaffAuth
import dev.profunktor.auth.jwt.{jwtEncode, JwtSecretKey, JwtToken}
import io.circe.syntax._
import pdi.jwt.{JwtAlgorithm, JwtClaim}

class TokensService[F[_]: Sync](
  config: UserJwt,
  tokenExpiration: TokenExpiration
) extends TokensAlgebra[F] {

  override def create(staff: StaffAuth): F[JwtToken] =
    Sync[F].delay(java.time.Clock.systemUTC).flatMap { implicit clock =>
      for {
        claim    <- Sync[F].delay(JwtClaim(staff.asJson.noSpaces).issuedNow.expiresIn(tokenExpiration.value.toMillis))
        secretKey = JwtSecretKey(config.secretKey.value)
        token    <- jwtEncode[F](claim, secretKey, JwtAlgorithm.HS256)
      } yield token
    }

}
