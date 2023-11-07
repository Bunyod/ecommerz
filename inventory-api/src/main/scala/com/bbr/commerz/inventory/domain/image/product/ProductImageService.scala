package com.bbr.commerz.inventory.domain.image.product

import cats.effect._
import cats.implicits._
import com.bbr.commerz.inventory.domain.product.ProductAlgebra
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Product.ProductId
import com.bbr.platform.effekts.GenUUID
import org.http4s.multipart.Part

class ProductImageService[F[_]: Async](mediaAlgebra: ProductImageAlgebra[F], productAlgebra: ProductAlgebra[F]) {
  def store(part: Part[F], orgId: OrgId, productId: ProductId): F[Unit] =
    (for {
      filename    <- Resource.eval(GenUUID.forSync.make)
      key          = s"/products/${orgId.value}/${filename.toString}"
      inputStream <- part.body.through(fs2.io.toInputStream).compile.resource.lastOrError
    } yield (key, inputStream)).evalMap { case (key, inputStream) =>
      mediaAlgebra.putS3Object(key, inputStream) *>
        productAlgebra.updateProductImage(productId.value, key)
    }.use_

  def createBucket(bucketName: String): F[Unit] =
    mediaAlgebra.createBucket(bucketName)

  def getObjectFile(productId: ProductId): F[Array[Byte]] =
    for {
      imagePath <- productAlgebra.getProductImagePath(productId)
      res       <- mediaAlgebra.getObjectFile(imagePath)
    } yield res
}
