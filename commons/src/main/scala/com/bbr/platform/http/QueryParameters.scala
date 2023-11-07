package com.bbr.platform.http

import cats.implicits._
import eu.timepit.refined._
import eu.timepit.refined.api.{Refined, Validate}
import org.http4s._
import org.http4s.dsl.Http4sDsl

import java.util.UUID
import scala.util.{Failure, Success, Try}

trait QueryParameters[F[_]] extends Http4sDsl[F] {

  import QueryParameters.uuidParamDecoder

  object NameQueryParamMatcher      extends OptionalQueryParamDecoderMatcher[String]("name")
  object UserNameQueryParamMatcher  extends OptionalQueryParamDecoderMatcher[String]("username")
  object CodeQueryParamMatcher      extends OptionalQueryParamDecoderMatcher[String]("code")
  object PriceFromQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Double]("price_from")
  object PriceToQueryParamMatcher   extends OptionalQueryParamDecoderMatcher[Double]("price_to")
  object LimitQueryParamMatcher     extends OptionalQueryParamDecoderMatcher[Int]("limit")
  object OffsetQueryParamMatcher    extends OptionalQueryParamDecoderMatcher[Int]("offset")
  object OrderIdQueryParamMatcher   extends OptionalQueryParamDecoderMatcher[UUID]("order_id")

}

object QueryParameters {

  implicit val uuidParamDecoder: QueryParamDecoder[UUID] =
    QueryParamDecoder.fromUnsafeCast(s =>
      Try(UUID.fromString(s.value)) match {
        case Failure(error) => throw error
        case Success(uuid)  => uuid
      }
    )("order_id")

  implicit def refinedParamDecoder[T: QueryParamDecoder, P](implicit
    env: Validate[T, P]
  ): QueryParamDecoder[T Refined P] =
    QueryParamDecoder[T].emap(
      refineV[P](_).leftMap(m => ParseFailure(m, m))
    )

}
