package app.backend

import app.backend.events.AppEndOutletEventConsumer
import app.backend.http.{AppServer, ChargingRoutes, CustomerRoutes}
import app.backend.services.{DynamoDBChargingService, DynamoDBCustomerService}
import nl.vroste.zio.kinesis.client.zionative.leaserepository.DynamoDbLeaseRepository
import shared.events.{DeadLetterProducer, OutletEventProducer}
import zio._
import zio.aws.core.config.AwsConfig
import zio.aws.dynamodb.DynamoDb
import zio.aws.kinesis.Kinesis
import zio.aws.netty.NettyHttpClient
import zio.dynamodb.DynamoDBExecutor

object Main extends ZIOAppDefault {

  val program =
    ZIO.serviceWithZIO[AppServer](_.start).zipPar(ZIO.serviceWithZIO[AppEndOutletEventConsumer](_.start))

  override def run: URIO[Any, ExitCode] =
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
        AppEndOutletEventConsumer.live,
        DeadLetterProducer.live,
        DeadLetterProducer.make,
        DynamoDbLeaseRepository.live,
        Kinesis.live,
        OutletEventProducer.live,
        OutletEventProducer.make,
        // http
        AppServer.live,
        ChargingRoutes.live,
        CustomerRoutes.live,
        // zio
        Scope.default
      )
      .exitCode
}

/*
  sbt run -jvm-debug 9999

  Consumer client initiates

  - initiate charging
             consumer client  -> consumer backend -> post - start / stop charging for consumer at device
             consumer backend -> charger backend  -> send - start / stop charging for consumer at device
             charger backend  -> charger device   -> push - change status

  - process billing
  if start - charger backend  -> billing          -> send - initiate charging session
  if stop  - charger backend  -> billing          -> send - charging session complete
             charger backend  -> consumer backend -> send - ack start / stop
             consumer backend -> consumer client  -> push - change status
  if stop  - consumer client  -> billing          -> get  - tally


  Charger device initiates

  - initiate charging
             charger device   -> charger backend  -> post - start / stop charging for consumer at device
             charger backend  -> consumer backend -> send - start / stop charging for consumer at device

  - process billing
  same as when Consumer client initiates
  except that the ack start / stop is missing
 */
