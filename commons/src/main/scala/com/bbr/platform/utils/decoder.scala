package com.bbr.platform.utils

import cats.MonadThrow
import cats.implicits._
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.collection.Size
import eu.timepit.refined.refineV
import io.circe.Decoder
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

object decoder {

  implicit def validateSizeN[N <: Int, R](implicit w: ValueOf[N]): Validate.Plain[R, Size[N]] =
    Validate.fromPredicate[R, Size[N]](
      _.toString.length == w.value,
      _ => s"Must have ${w.value} digits",
      Size[N](w.value)
    )

  implicit class RefinedRequestDecoder[F[_]: JsonDecoder: MonadThrow](request: Request[F]) extends Http4sDsl[F] {
    def decodeR[A: Decoder](f: A => F[Response[F]]): F[Response[F]] =
      request.asJsonDecode[A].attempt.flatMap {
        case Left(e)  =>
          Option(e.getCause) match {
            case Some(c) if c.getMessage.startsWith("Predicate") => BadRequest(c.getMessage)
            case error                                           => UnprocessableEntity(error.map(_.getMessage))
          }
        case Right(a) => f(a)
      }
  }

  def decoderOf[T, P](implicit v: Validate[T, P], d: Decoder[T]): Decoder[T Refined P] =
    d.emap(refineV[P].apply[T](_))

}
