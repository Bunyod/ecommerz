package com.bbr.commerz.sales

import cats.effect._
import com.bbr.commerz.auth.domain.token.TokensService
import com.bbr.commerz.organization.domain.staff.StaffPayloads.UserJwtAuth
import com.bbr.commerz.inventory.resources.MkAwsServices
import com.bbr.commerz.sales.config.Config
import com.bbr.commerz.sales.resources.{AppResources, MkApiServices, MkHttpServer}
import com.bbr.commerz.sales.services.{Repositories, Services}
import com.bbr.platform.crypto.CryptoService
import dev.profunktor.auth.jwt.JwtAuth
import dev.profunktor.redis4cats.effect.Log.Stdout._
import eu.timepit.refined.auto._
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pdi.jwt.JwtAlgorithm

object Bootstrap extends IOApp.Simple {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] = {
    val server = for {
      cfg           <- Resource.eval(Config.load[IO])
      res           <- AppResources.make[IO](cfg)
      tr            <- res.psql
      s3            <- MkAwsServices[IO].startAwsS3(cfg.aws)
      userJwtAuth    = UserJwtAuth(JwtAuth.hmac(cfg.userJwt.secretKey, JwtAlgorithm.HS256))
      cryptoService <- CryptoService.make[IO](cfg.passwordSalt).toResource
      tokensService  = new TokensService[IO](cfg.userJwt, cfg.tokenExpiration)
      repositories   = Repositories.make[IO](cfg, tr, res.redis, tokensService, s3)
      services       = Services.make[IO](repositories, cryptoService)
      httpApi       <- MkApiServices[IO].startApiServices(res, services, tr, userJwtAuth)
      srv           <- MkHttpServer[IO].newEmber(cfg.httpServer, httpApi.httpApp)
    } yield srv
    server.useForever
  }
}
