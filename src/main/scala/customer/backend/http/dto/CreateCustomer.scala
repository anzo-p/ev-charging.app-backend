package customer.backend.http.dto

import customer.backend.types.customer._
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

final case class CreateCustomer(
    customerId: Option[UUID],
    rfidTag: Option[String],
    address: String,
    email: String,
    paymentMethod: String
  ) {

  def toModel: Customer =
    Customer(
      address,
      email,
      paymentMethod
    )
}

object CreateCustomer {
  implicit val codec: JsonCodec[CreateCustomer] = DeriveJsonCodec.gen[CreateCustomer]

  def fromModel(model: Customer): CreateCustomer =
    CreateCustomer(
      Some(model.customerId),
      Some(model.rfidTag),
      model.address,
      model.email,
      model.paymentMethod
    )
}
