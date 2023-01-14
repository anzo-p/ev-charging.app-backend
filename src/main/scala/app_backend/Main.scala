package app_backend

import app_backend.events.KinesisChargingEventsIn
import app_backend.http.{AppServer, ChargingRoutes, CustomerRoutes}
import app_backend.services.{DynamoDBChargingService, DynamoDBCustomerService}
import nl.vroste.zio.kinesis.client.zionative.leaserepository.DynamoDbLeaseRepository
import shared.events.kinesis.{KinesisChargingEventsOut, KinesisDeadLetters}
import zio._
import zio.aws.core.config.AwsConfig
import zio.aws.dynamodb.DynamoDb
import zio.aws.kinesis.Kinesis
import zio.aws.netty.NettyHttpClient
import zio.dynamodb.DynamoDBExecutor

object Main extends ZIOAppDefault {

  val program =
    ZIO.serviceWithZIO[AppServer](_.start).zipPar(ZIO.serviceWithZIO[KinesisChargingEventsIn](_.start))

  val setup =
    program
      .provide(
        // aws config
        AwsAccessConfig.live,
        AwsConfig.configured(),
        NettyHttpClient.default,
        // dynamodb
        DynamoDb.live,
        DynamoDBChargingService.live,
        DynamoDBCustomerService.live,
        DynamoDBExecutor.live,
        // kinesis
        DynamoDbLeaseRepository.live,
        Kinesis.live,
        KinesisChargingEventsIn.live,
        KinesisChargingEventsOut.live,
        KinesisChargingEventsOut.make,
        KinesisDeadLetters.live,
        KinesisDeadLetters.make,
        // http
        AppServer.live,
        ChargingRoutes.live,
        CustomerRoutes.live,
        // zio
        Scope.default
      )

  override def run: URIO[Any, ExitCode] =
    setup.catchAll {
      case throwable: Throwable => ZIO.succeed(println(throwable.getMessage))
      case _                    => ZIO.succeed(())
    }.exitCode
}
