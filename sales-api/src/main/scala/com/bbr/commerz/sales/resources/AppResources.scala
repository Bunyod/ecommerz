package com.bbr.commerz.sales.resources

import cats.effect._
import cats.implicits._
import com.bbr.platform.config.Configuration.{PostgresCfg, RedisCfg, ServiceConfig}
import dev.profunktor.redis4cats.effect.MkRedis
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import org.http4s.client.Client
import org.typelevel.log4cats.Logger

sealed abstract class AppResources[F[_]](
  val client: Client[F],
  val psql: Resource[F, Transactor[F]],
  val redis: RedisCommands[F, String, String]
)

object AppResources {

  def make[F[_]: Async: Logger: MkHttpClient: MkRedis](
    cfg: ServiceConfig
  ): Resource[F, AppResources[F]] = {

    def dbTransactor(
      dbc: PostgresCfg
    ): Resource[F, HikariTransactor[F]] =
      for {
        connectEc  <- ExecutionContexts.fixedThreadPool[F](dbc.connections.poolSize.value)
        transactor <- HikariTransactor
                        .newHikariTransactor[F](
                          dbc.driver.value,
                          dbc.jdbcUrl.value,
                          dbc.user.value,
                          dbc.password.value,
                          connectEc
                        )
      } yield transactor

    def checkRedisConnection(
      redis: RedisCommands[F, String, String]
    ): F[Unit] =
      redis.info.flatMap {
        _.get("redis_version").traverse_ { v =>
          Logger[F].info(s"Connected to Redis $v")
        }
      }

    def mkRedisResource(c: RedisCfg): Resource[F, RedisCommands[F, String, String]] =
      Redis[F].utf8(c.uri.value).evalTap(checkRedisConnection)

    (
      MkHttpClient[F].newEmber(cfg.httpClient),
      mkRedisResource(cfg.redis)
    ).parMapN(new AppResources[F](_, dbTransactor(cfg.postgres), _) {})
  }
}
