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
        AwsConfig.default,
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

/*
  TODO

  some of the SQS consumer failures dont show up anywhere
  - rename data to event in all consumers
  - when outletState match against option that isnt found
  - when state transition check failed

  SQSOutletDeviceMessagesIn.DeviceRequestsCharging payload without rfidTag does not fail but also doesnt do anything else either

  rest api route hierarchy needs be fixed

  ChargingSession.purchaseChannel is wrongly presented in db, now json, should be scalar

  handler logics are a bit spread out between controllers, models, handlers and services, which themselves are a mix between repo and service

  there is no definitive way of passing handler errors as reponses to callers for change

  Kinesis checkpoints
 */
