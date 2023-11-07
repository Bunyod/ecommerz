package com.bbr.commerz.sales.resources

import cats.effect._
import com.bbr.platform.config.Configuration.HttpClientCfg
import fs2.io.net.Network
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

trait MkHttpClient[F[_]] {
  def newEmber(c: HttpClientCfg): Resource[F, Client[F]]
}

object MkHttpClient {
  def apply[F[_]: MkHttpClient]: MkHttpClient[F] = implicitly

  implicit def forAsync[F[_]: Async: Network]: MkHttpClient[F] =
    new MkHttpClient[F] {
      override def newEmber(c: HttpClientCfg): Resource[F, Client[F]] =
        EmberClientBuilder
          .default[F]
          .withTimeout(c.connectionTimeout)
          .withIdleTimeInPool(c.requestTimeout)
          .build
    }
}
