package app.backend.http.dto

import app.backend.types.chargingSession.ChargingSession
import shared.types.enums.PurchaseChannel
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

final case class CreateChargingSessionDto(
    sessionId: Option[UUID],
    customerId: UUID,
    outletId: UUID,
    purchaseChannel: Option[String]
  ) {

  def toModel: ChargingSession =
    ChargingSession(this.customerId, this.outletId, PurchaseChannel.MobileApp)
}

object CreateChargingSessionDto {
  implicit val codec: JsonCodec[CreateChargingSessionDto] =
    DeriveJsonCodec.gen[CreateChargingSessionDto]
}

final case class ChargingSessionDto(
    sessionId: UUID,
    customerId: UUID,
    outletId: UUID,
    state: String,
    purchaseChannel: String,
    startTime: java.time.OffsetDateTime,
    endTime: Option[java.time.OffsetDateTime],
    powerConsumption: Double
  )

object ChargingSessionDto {
  implicit val codec: JsonCodec[ChargingSessionDto] =
    DeriveJsonCodec.gen[ChargingSessionDto]

  def fromModel(model: ChargingSession): ChargingSessionDto =
    ChargingSessionDto(
      sessionId        = model.sessionId,
      customerId       = model.customerId,
      outletId         = model.outletId,
      state            = model.outletState.entryName,
      purchaseChannel  = model.purchaseChannel.entryName,
      startTime        = model.startTime,
      endTime          = model.endTime,
      powerConsumption = model.powerConsumption
    )
}
