package com.bbr.commerz.inventory.resources

import cats.effect._
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.bbr.platform.config.Config.AwsS3Cfg

trait MkAwsServices[F[_]] {
  def startAwsS3(cfg: AwsS3Cfg): Resource[F, AmazonS3]
}

object MkAwsServices {
  def apply[F[_]: MkAwsServices]: MkAwsServices[F] = implicitly

  implicit def forAsyncLogger[F[_]]: MkAwsServices[F] = new MkAwsServices[F] {
    override def startAwsS3(cfg: AwsS3Cfg): Resource[F, AmazonS3] = {
      val awsCreds = new AWSStaticCredentialsProvider(new BasicAWSCredentials(cfg.keyId.value, cfg.password.value))
      Resource.pure[F, AmazonS3](
        AmazonS3ClientBuilder
          .standard()
          .withCredentials(awsCreds)
          .withRegion(Regions.fromName(cfg.region.value))
          .build()
      )
    }
  }
}
