package app_backend.types.chargingSession

import shared.types.enums.OutletDeviceState
import zio.schema.{DeriveSchema, Schema}

import java.util.UUID

object ChargingSessionProjections {

  final case class ChargingSessionsOfCustomer(customerId: UUID, sessionId: UUID, sessionState: OutletDeviceState)

  object ChargingSessionsOfCustomer {
    implicit lazy val schema: Schema[ChargingSessionsOfCustomer] =
      DeriveSchema.gen[ChargingSessionsOfCustomer]
  }
}
