package app_backend

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import zio._
import zio.aws.core.config.CommonAwsConfig
import zio.config._

object AwsAccessConfig {

  val live: ZLayer[Any, ReadError[String], CommonAwsConfig] =
    ZLayer.succeed {
      CommonAwsConfig(
        region              = Some(Region.EU_WEST_1),
        credentialsProvider = DefaultCredentialsProvider.create(),
        endpointOverride    = None,
        commonClientConfig  = None
      )
    }
}
