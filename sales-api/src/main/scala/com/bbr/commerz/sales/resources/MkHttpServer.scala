package com.bbr.commerz.sales.resources

import cats.effect._
import cats.implicits._
import com.bbr.platform.config.Configuration.HttpServerCfg
import com.comcast.ip4s.{IpAddress, Port}
import fs2.io.net.Network
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.defaults.Banner
import org.typelevel.log4cats.Logger

trait MkHttpServer[F[_]] {
  def newEmber(cfg: HttpServerCfg, httpApp: HttpApp[F]): Resource[F, Server]
}

object MkHttpServer {
  def apply[F[_]: MkHttpServer]: MkHttpServer[F] = implicitly

  private def showEmberBanner[F[_]: Logger](s: Server): F[Unit] =
    Logger[F].info(s"\n${Banner.mkString("\n")}\nHTTP Server started at ${s.address}")

  implicit def forAsyncLogger[F[_]: Async: Logger: Network]: MkHttpServer[F] =
    new MkHttpServer[F] {
      def newEmber(cfg: HttpServerCfg, httpApp: HttpApp[F]): Resource[F, Server] =
        (
          IpAddress.fromString(cfg.host.value),
          Port.fromInt(cfg.port.value)
        ).mapN { (host, port) =>
          EmberServerBuilder
            .default[F]
            .withHost(host)
            .withPort(port)
            .withHttpApp(httpApp)
            .build
            .evalTap(showEmberBanner[F])
        }.getOrElse(Resource.raiseError(new Throwable("Couldn't load server configurations")))
    }
}
