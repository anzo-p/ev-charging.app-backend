package app_backend.types.customer

import zio.schema.{DeriveSchema, Schema}

import java.util.UUID

final case class Customer(
    customerId: UUID,
    rfidTag: String,
    address: String,
    email: String,
    paymentMethod: String
  )

object Customer {
  implicit lazy val schema: Schema[Customer] =
    DeriveSchema.gen[Customer]

  def apply(address: String, email: String, paymentMethod: String): Customer =
    Customer(
      customerId    = UUID.randomUUID(),
      rfidTag       = UUID.randomUUID().toString,
      address       = address,
      email         = email,
      paymentMethod = paymentMethod
    )
}
