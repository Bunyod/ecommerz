package com.bbr.commerz.sales.resources

import cats.effect._
import com.bbr.commerz.organization.domain.staff.StaffPayloads.UserJwtAuth
import com.bbr.commerz.sales.services.Services
import com.bbr.commerz.sales.http.HttpApi
import doobie.Transactor

trait MkApiServices[F[_]] {
  def startApiServices(
    res: AppResources[F],
    services: Services[F],
    transactor: Transactor[F],
    userJwtAuth: UserJwtAuth
  ): Resource[F, HttpApi[F]]
}

object MkApiServices {

  def apply[F[_]: MkApiServices]: MkApiServices[F] = implicitly

  implicit def forAsyncLogger[F[_]: Async]: MkApiServices[F] = new MkApiServices[F] {
    override def startApiServices(
      res: AppResources[F],
      services: Services[F],
      transactor: Transactor[F],
      userJwtAuth: UserJwtAuth
    ): Resource[F, HttpApi[F]] =
      Resource.pure {
        new HttpApi[F](services, userJwtAuth)
      }
  }

}
