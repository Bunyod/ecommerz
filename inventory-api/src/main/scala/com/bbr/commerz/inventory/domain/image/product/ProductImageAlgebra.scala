package com.bbr.commerz.inventory.domain.image.product

import java.io.InputStream

trait ProductImageAlgebra[F[_]] {
  def createBucket(bucketName: String): F[Unit]
  def putS3Object(filename: String, is: InputStream, contentType: String = "image/jpeg"): F[String]
  def getObjectFile(filePath: String, fileExtension: String = "jpg"): F[Array[Byte]]
}
