package app_backend.services

import app_backend.ChargingService
import app_backend.types.chargingSession.ChargingSession
import app_backend.types.chargingSession.ChargingSessionProjections.ChargingSessionsOfCustomer
import shared.db.DynamoDBPrimitives
import shared.types.TimeExtensions._
import shared.types.chargingEvent.ChargingEvent
import shared.types.enums.OutletDeviceState
import shared.types.outletStateMachine.OutletStateMachine._
import zio.dynamodb.DynamoDBQuery._
import zio.dynamodb.PartitionKeyExpression.PartitionKey
import zio.dynamodb.ProjectionExpression.$
import zio.dynamodb._
import zio.schema.{DeriveSchema, Schema}
import zio.{Task, ZIO, ZLayer}

import java.util.UUID
import scala.concurrent.duration.DurationInt

final case class DynamoDBChargingService(executor: DynamoDBExecutor)
    extends ChargingService
    with DynamoDBPrimitives[ChargingSession]
    with DateTimeSchemaImplicits {

  val tableResource = "ev-charging_charging-session_table"
  val primaryKey    = "sessionId"

  override def schema: Schema[ChargingSession] = DeriveSchema.gen[ChargingSession]

  private def getActiveSession(customerId: UUID, outletId: UUID): ZIO[DynamoDBExecutor, Throwable, Option[UUID]] =
    for {
      query <- queryAll[ChargingSessionsOfCustomer](tableResource, $("outletId"), $("outletState"), $("startTime"), $("sessionId"))
                .indexName("ev-charging_active-charging-sessions_index")
                .whereKey(PartitionKey("customerId") === customerId.toString)
                // FIXME SortKey desc over startTime against now().minusHours(..) would be necessary
                // but aws error indicates that OffsetDateTime gets serialized as string, which cannot be sorted over as temporal quality
                .execute

      result <- query
                 .runCollect
                 .map(
                   _.toList
                     .filter(_.outletId == outletId)
                     .filter(_.startTime.isRecentUpTo(2.minutes))
                     .find(_.outletState.in(Seq(
                       OutletDeviceState.AppRequestsCharging,
                       OutletDeviceState.DeviceRequestsCharging,
                       OutletDeviceState.Charging,
                       OutletDeviceState.AppRequestsStop,
                       OutletDeviceState.DeviceRequestsStop
                     )))
                     .map(_.sessionId))
    } yield result

  override def getHistory(customerId: UUID): Task[List[ChargingSession]] =
    (for {
      query <- queryAll[ChargingSession](tableResource)
                .whereKey(PartitionKey("customerId") === customerId.toString)
                .execute

      result <- query.runCollect
    } yield result
      .sortBy(_.startTime)
      .toList
      .reverse)
      .provideLayer(ZLayer.succeed(executor))

  override def initialize(session: ChargingSession): Task[UUID] =
    (for {
      session <- getActiveSession(session.customerId, session.outletId).flatMap {
                  case Some(sessionId) => getByPK(sessionId)
                  case None            => ZIO.succeed(session)
                }

      _ <- put(tableResource, session).execute
    } yield session.sessionId)
      .provideLayer(ZLayer.succeed(executor))

  override def get(sessionId: UUID): Task[ChargingSession] =
    getByPK(sessionId)
      .provideLayer(ZLayer.succeed(executor))

  override def updateSession(event: ChargingEvent): Task[Unit] =
    (for {
      sessionId <- ZIO.from(event.recentSession.sessionId).orElseFail(new Error("no session id"))
      data      <- getByPK(sessionId).filterOrFail(_.mayTransitionTo(event.outletState))(new Error(cannotTransitionTo(event.outletState)))

      _ <- putByPK(
            data.copy(
              outletState      = event.outletState,
              endTime          = Some(event.recentSession.periodEnd.getOrElse(java.time.OffsetDateTime.now())),
              powerConsumption = event.recentSession.powerConsumption
            ))

    } yield ())
      .provideLayer(ZLayer.succeed(executor))

  override def setStopRequested(sessionId: UUID): Task[Unit] = {
    val targetState = OutletDeviceState.AppRequestsStop
    (for {
      data <- getByPK(sessionId).filterOrFail(_.mayTransitionTo(targetState))(new Error(cannotTransitionTo(targetState)))
      _    <- putByPK(data.copy(outletState = targetState))
    } yield ())
      .provideLayer(ZLayer.succeed(executor))
  }
}

object DynamoDBChargingService {

  val live: ZLayer[DynamoDBExecutor, Nothing, ChargingService] =
    ZLayer.fromFunction(DynamoDBChargingService.apply _)
}
