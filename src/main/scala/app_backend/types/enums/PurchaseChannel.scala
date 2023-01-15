package app_backend.types.enums

import enumeratum.{Enum, EnumEntry}
import zio.json.{JsonDecoder, JsonEncoder}
import zio.schema.Schema

sealed trait PurchaseChannel extends EnumEntry

object PurchaseChannel extends Enum[PurchaseChannel] {
  val values: IndexedSeq[PurchaseChannel] = findValues

  implicit val decoder: JsonDecoder[PurchaseChannel] =
    JsonDecoder[String].map(PurchaseChannel.withName)

  implicit val encoder: JsonEncoder[PurchaseChannel] =
    JsonEncoder[String].contramap(_.entryName)

  implicit val schema: Schema[PurchaseChannel] =
    Schema[String].transform(
      PurchaseChannel.withName,
      _.entryName
    )

  case object OutletDevice extends PurchaseChannel
  case object MobileApp extends PurchaseChannel
}
