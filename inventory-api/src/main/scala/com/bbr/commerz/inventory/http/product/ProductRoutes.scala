package com.bbr.commerz.inventory.http.product

import cats.effect.Async
import cats.implicits._
import com.bbr.commerz.inventory.domain.image.product.ProductImageService
import com.bbr.commerz.inventory.domain.product.ProductService
import com.bbr.platform.utils.decoder._
import com.bbr.platform.domain.Product.ProductId
import com.bbr.commerz.inventory.domain.product.ProductPayloads._
import com.bbr.commerz.inventory.http.utils.json._
import com.bbr.platform.UuidOpts
import com.bbr.platform.domain.Staff.StaffAuth
import com.bbr.platform.domain.Staff.StaffRole.{OWNER, WORKER}
import com.bbr.platform.http.QueryParameters
import org.http4s.EntityDecoder.multipart
import org.http4s.circe._
import org.http4s.headers.`Content-Type`
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes, MediaType}

final class ProductRoutes[F[_]: Async: JsonDecoder](
  productService: ProductService[F],
  productImageService: ProductImageService[F]
) extends QueryParameters[F] {

  val httpRoutes: AuthedRoutes[StaffAuth, F] = AuthedRoutes.of {

    case ar @ POST -> Root / UUIDVar(orgId) / "inventory" / "products" / UUIDVar(productId) / "multipart"
        as StaffAuth(_, _, _, WORKER | OWNER) =>
      ar.req.decodeWith(decoder = multipart[F], strict = true) { response =>
        val stream = response.parts.headOption
          .traverse(p => productImageService.store(p, orgId.toOrgId, ProductId(productId)))
        Ok(stream.map(_ => s"Multipart file parsed successfully > ${response.parts}"))
          .recoverWith(er => InternalServerError(er.getMessage))
      }

    case GET -> Root / UUIDVar(_) / "inventory" / "products" / UUIDVar(productId) / "image" as _ =>
      Ok(productImageService.getObjectFile(ProductId(productId)))
        .map(_.withContentType(`Content-Type`(MediaType.image.png)))
        .recoverWith(er => InternalServerError(er.getMessage))

    case ar @ POST -> Root / UUIDVar(orgId) / "inventory" / "products" as StaffAuth(_, _, _, WORKER | OWNER) =>
      ar.req.decodeR[ProductRequest] { product =>
        Created(productService.create(orgId = orgId.toOrgId, product = product))
          .recoverWith {
            case er if er.getMessage.contains("already exists") => BadRequest(er.getMessage)
            case er                                             => InternalServerError(er.getMessage)
          }
      }

    case ar @ PUT -> Root / UUIDVar(orgId) / "inventory" / "products" / UUIDVar(id)
        as StaffAuth(_, _, _, WORKER | OWNER) =>
      ar.req.decodeR[ProductUpdate] { product =>
        Ok(productService.updateById(orgId.toOrgId, ProductId(id), product))
          .recoverWith(er => InternalServerError(er.getMessage))
      }

    case GET -> Root / UUIDVar(orgId) / "inventory" / "products" / UUIDVar(id) as _ =>
      Ok(productService.getById(orgId.toOrgId, ProductId(id)))
        .recoverWith(er => InternalServerError(er.getMessage))

    case GET -> Root / UUIDVar(orgId) / "inventory" / "products" :? NameQueryParamMatcher(
          name
        ) +& OffsetQueryParamMatcher(
          offset
        ) +& CodeQueryParamMatcher(code) +& PriceFromQueryParamMatcher(
          priceFrom
        ) +& PriceToQueryParamMatcher(priceTo) +& LimitQueryParamMatcher(
          limit
        ) as _ =>
      Ok(
        productService.getByParams(
          orgId.toOrgId,
          name,
          code,
          priceFrom,
          priceTo,
          limit,
          offset
        )
      ).recoverWith(er => InternalServerError(er.getMessage))

    case DELETE -> Root / UUIDVar(orgId) / "inventory" / "products" / UUIDVar(id)
        as StaffAuth(_, _, _, WORKER | OWNER) =>
      Ok(productService.deleteById(orgId.toOrgId, ProductId(id)))
        .recoverWith(er => InternalServerError(er.getMessage))
  }

  private val prefixPath = "/org"

  def routes(authMiddleware: AuthMiddleware[F, StaffAuth]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
