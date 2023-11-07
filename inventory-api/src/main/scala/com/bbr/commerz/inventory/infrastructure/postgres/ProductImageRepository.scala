package com.bbr.commerz.inventory.infrastructure.postgres

import cats.effect.Async
import cats.implicits._
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import com.bbr.commerz.inventory.domain.image.product.ProductImageAlgebra
import com.bbr.platform.config.Config.AwsS3Cfg

import java.io.{File, FileInputStream, InputStream}
import scala.util.{Failure, Success, Try}

class ProductImageRepository[F[_]: Async](s3: AmazonS3, cfg: AwsS3Cfg) extends ProductImageAlgebra[F] {

  override def createBucket(bucketName: String): F[Unit] =
    if (s3.doesBucketExistV2(bucketName)) {
      new Throwable(s"Bucket bucketName already exists").raiseError[F, Unit]
    } else {
      Try(s3.createBucket(bucketName)).toEither match {
        case Right(_)    => ().pure[F]
        case Left(value) => value.raiseError[F, Unit]
      }
    }

  override def putS3Object(filename: String, is: InputStream, contentType: String = "image/jpeg"): F[String] = {
    val objectMetadata   = new ObjectMetadata()
    objectMetadata.setContentType(contentType)
    val putObjectRequest = new PutObjectRequest(cfg.bucket.value, filename, is, objectMetadata)
    val manager          = TransferManagerBuilder.standard().withS3Client(s3).build()
    Try(manager.upload(putObjectRequest).waitForUploadResult().getKey) match {
      case Success(value)     =>
        manager.shutdownNow()
        value.pure[F]
      case Failure(exception) =>
        manager.shutdownNow()
        exception.raiseError[F, String]
    }
  }

  override def getObjectFile(imagePath: String, fileExtension: String = "jpg"): F[Array[Byte]] = {
    val file    = new File(s"""${imagePath.split("/").lastOption.getOrElse("image")}.$fileExtension""")
    val manager = TransferManagerBuilder.standard().withS3Client(s3).build()
    Try(manager.download(cfg.bucket.value, imagePath, file).waitForCompletion()) match {
      case Success(_)         =>
        manager.shutdownNow()
        val bytesArray      = new Array[Byte](file.length.asInstanceOf[Int])
        val fileInputStream = new FileInputStream(file)
        fileInputStream.read(bytesArray)
        bytesArray.pure[F]
      case Failure(exception) =>
        manager.shutdownNow()
        exception.raiseError[F, Array[Byte]]
    }

  }

}
