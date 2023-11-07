package com.bbr.commerz.sales.config

import cats.effect.Async
import cats.syntax.all._
import ciris._
import ciris.refined._
import com.bbr.platform.config.Configuration._
import com.bbr.platform.config.Config.{AppEnvironment, AwsS3Cfg}
import eu.timepit.refined.auto._
import eu.timepit.refined.types.net.UserPortNumber
import eu.timepit.refined.types.string.NonEmptyString
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import eu.timepit.refined.pureconfig._

import scala.concurrent.duration._

object Config {

  // Ciris promotes configuration as code
  def load[F[_]: Async]: F[ServiceConfig] =
    env("APP_ENV")
      .as[AppEnvironment]
      .option
      .flatMap {
        case Some(AppEnvironment.Local) | None =>
          ConfigValue.default[ServiceConfig](ConfigSource.default.loadOrThrow[ServiceConfig])
        case _                                 =>
          (
            env("USER_TOKEN").as[NonEmptyString],
            env("PASSWORD_SALT").as[NonEmptyString],
            env("REDIS_URI").as[NonEmptyString],
            env("POSTGRESQL_DRIVER").as[NonEmptyString],
            env("POSTGRESQL_HOST").as[NonEmptyString],
            env("POSTGRESQL_PORT").as[UserPortNumber],
            env("POSTGRESQL_USER").as[NonEmptyString],
            env("POSTGRESQL_PASSWORD").as[NonEmptyString],
            env("POSTGRESQL_DATABASE").as[NonEmptyString],
            env("POSTGRESQL_JDBC_URL").as[NonEmptyString],
            env("AWS_S3_KEY_ID").as[NonEmptyString],
            env("AWS_S3_PASSWORD").as[NonEmptyString],
            env("AWS_S3_REGION").as[NonEmptyString],
            env("AWS_S3_BUCKET").as[NonEmptyString]
          ).parMapN {
            (
              userToken,
              salt,
              redisUri,
              driver,
              pgHost,
              pgPort,
              pgUser,
              pgPassword,
              pgDb,
              jdbc,
              s3KeyId,
              s3Password,
              s3Region,
              s3Bucket
            ) =>
              ServiceConfig(
                userJwt = UserJwt(userToken),
                passwordSalt = PasswordSalt(salt),
                tokenExpiration = TokenExpiration(30.minutes),
                cartExpiration = CartExpiration(30.minutes),
                httpClient = HttpClientCfg(
                  connectionTimeout = 2.seconds,
                  requestTimeout = 2.seconds
                ),
                httpServer = HttpServerCfg(
                  host = "0.0.0.0",
                  port = 8080
                ),
                postgres = PostgresCfg(
                  driver = driver,
                  host = pgHost,
                  port = pgPort,
                  user = pgUser,
                  schema = pgDb,
                  password = pgPassword,
                  jdbcUrl = jdbc,
                  connections = DbConnectionsCfg(10)
                ),
                redis = RedisCfg(redisUri),
                CheckoutConfig(
                  retriesLimit = 3,
                  retriesBackoff = 10.milliseconds
                ),
                aws = AwsS3Cfg(
                  keyId = s3KeyId,
                  password = s3Password,
                  region = s3Region,
                  bucket = s3Bucket
                )
              )
          }
      }
      .load[F]
}
