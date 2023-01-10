package app.backend.types.chargingSession

import shared.types.TimeExtensions.DateTimeSchemaImplicits
import shared.types.chargingEvent.{ChargingEvent, EventSession}
import shared.types.enums.{EventInitiator, OutletDeviceState, PurchaseChannel}
import shared.types.outletStateMachine.OutletStateMachine
import zio.schema.{DeriveSchema, Schema}

import java.util.UUID

final case class ChargingSession(
    sessionId: UUID,
    customerId: UUID,
    rfidTag: String,
    outletId: UUID,
    outletState: OutletDeviceState,
    purchaseChannel: PurchaseChannel,
    startTime: java.time.OffsetDateTime,
    endTime: Option[java.time.OffsetDateTime],
    powerConsumption: Double
  ) extends OutletStateMachine {

  def toEvent: ChargingEvent =
    ChargingEvent(
      initiator   = EventInitiator.AppBackend,
      outletId    = outletId,
      outletState = outletState,
      recentSession = EventSession(
        sessionId        = Some(sessionId),
        rfidTag          = rfidTag,
        periodStart      = startTime,
        periodEnd        = endTime,
        powerConsumption = powerConsumption
      )
    )
}

object ChargingSession extends DateTimeSchemaImplicits {

  implicit lazy val schema: Schema[ChargingSession] =
    DeriveSchema.gen[ChargingSession]

  def apply(customerId: UUID, outletId: UUID, purchaseChannel: PurchaseChannel): ChargingSession =
    ChargingSession(
      sessionId        = UUID.randomUUID(),
      customerId       = customerId,
      rfidTag          = "",
      outletId         = outletId,
      outletState      = OutletDeviceState.AppRequestsCharging,
      purchaseChannel  = purchaseChannel,
      startTime        = java.time.OffsetDateTime.now(),
      endTime          = None,
      powerConsumption = 0.0
    )

  def fromEvent(customerId: UUID, event: ChargingEvent): ChargingSession =
    ChargingSession(
      sessionId        = event.recentSession.sessionId.getOrElse(UUID.randomUUID()),
      customerId       = customerId,
      rfidTag          = event.recentSession.rfidTag,
      outletId         = event.outletId,
      outletState      = event.outletState,
      purchaseChannel  = PurchaseChannel.OutletDevice,
      startTime        = event.recentSession.periodStart,
      endTime          = event.recentSession.periodEnd,
      powerConsumption = event.recentSession.powerConsumption
    )
}
