package app_backend.types.customer

import zio.schema.{DeriveSchema, Schema}

import java.util.UUID

object CustomerProjections {

  final case class CustomerIdAndRfidTag(customerId: UUID, rfidTag: String)

  object CustomerIdAndRfidTag {
    implicit lazy val schema: Schema[CustomerIdAndRfidTag] =
      DeriveSchema.gen[CustomerIdAndRfidTag]
  }
}
