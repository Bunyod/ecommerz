package com.bbr.platform.config

import com.bbr.platform.config.Config.AwsS3Cfg
import eu.timepit.refined.types.all._
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString

import scala.concurrent.duration.FiniteDuration

object Configuration {

  case class ServiceConfig(
    userJwt: UserJwt,
    passwordSalt: PasswordSalt,
    tokenExpiration: TokenExpiration,
    cartExpiration: CartExpiration,
    httpClient: HttpClientCfg,
    httpServer: HttpServerCfg,
    postgres: PostgresCfg,
    redis: RedisCfg,
    checkoutConfig: CheckoutConfig,
    aws: AwsS3Cfg
  )

  final case class UserJwt(
    secretKey: NonEmptyString
  )

  final case class PasswordSalt(
    value: NonEmptyString
  )

  final case class TokenExpiration(
    value: FiniteDuration
  )

  final case class CartExpiration(value: FiniteDuration)

  final case class HttpClientCfg(
    connectionTimeout: FiniteDuration,
    requestTimeout: FiniteDuration
  )

  final case class HttpServerCfg(
    host: NonEmptyString,
    port: UserPortNumber
  )

  final case class DbConnectionsCfg(poolSize: PosInt)

  final case class PostgresCfg(
    driver: NonEmptyString,
    host: NonEmptyString,
    port: UserPortNumber,
    user: NonEmptyString,
    schema: NonEmptyString,
    password: NonEmptyString,
    jdbcUrl: NonEmptyString,
    connections: DbConnectionsCfg
  )

  final case class RedisCfg(uri: NonEmptyString)

  case class CheckoutConfig(
    retriesLimit: PosInt,
    retriesBackoff: FiniteDuration
  )

}
