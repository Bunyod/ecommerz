package com.bbr.platform.effekts

import cats.effect.Sync

import scala.util.Random

trait GenRecoveryCode[F[_]] {
  def make: F[Int]
}

object GenRecoveryCode {
  def apply[F[_]: GenRecoveryCode]: GenRecoveryCode[F] = implicitly

  implicit def forSync[F[_]: Sync]: GenRecoveryCode[F] =
    new GenRecoveryCode[F] {
      def make: F[Int] = Sync[F].delay(Random.nextInt(900000) + 100000)
    }
}
