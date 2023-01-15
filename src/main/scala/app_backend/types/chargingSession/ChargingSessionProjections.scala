package app_backend.types.chargingSession

import shared.types.TimeExtensions.DateTimeSchemaImplicits
import shared.types.enums.OutletDeviceState
import zio.schema.{DeriveSchema, Schema}

import java.util.UUID

object ChargingSessionProjections {

  final case class ChargingSessionsOfCustomer(
      outletId: UUID,
      outletState: OutletDeviceState,
      startTime: java.time.OffsetDateTime,
      sessionId: UUID
    )

  object ChargingSessionsOfCustomer extends DateTimeSchemaImplicits {
    implicit lazy val schema: Schema[ChargingSessionsOfCustomer] =
      DeriveSchema.gen[ChargingSessionsOfCustomer]
  }
}
