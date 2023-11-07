package com.bbr.commerz.auth.infrastructure

import cats.Monad
import cats.effect.Sync
import cats.implicits._
import com.bbr.commerz.auth.domain.auth.UserAuthAlgebra
import com.bbr.commerz.organization.http.utils.json.staffAuthDecoder
import com.bbr.platform.domain.Staff.StaffAuth
import dev.profunktor.auth.jwt._
import dev.profunktor.redis4cats.RedisCommands
import io.circe.parser.decode
import pdi.jwt._

object StaffAuthRepository {
  def make[F[_]: Sync](
    redis: RedisCommands[F, String, String]
  ): F[UserAuthAlgebra[F, StaffAuth]] =
    Sync[F].delay(
      new StaffAuthRepository(redis)
    )
}

class StaffAuthRepository[F[_]: Monad](
  redis: RedisCommands[F, String, String]
) extends UserAuthAlgebra[F, StaffAuth] {

  override def findUser(jwtToken: JwtToken)(claim: JwtClaim): F[Option[StaffAuth]] =
    decode[StaffAuth](claim.content).toOption.flatTraverse { staff =>
      redis
        .get(staff.phoneNumber.value)
        .flatMap(_.flatTraverse { u =>
          if (u == jwtToken.value) {
            staff.some.pure[F]
          } else {
            none[StaffAuth].pure[F]
          }
        })
    }

}
