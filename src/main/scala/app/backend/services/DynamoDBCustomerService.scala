package app.backend.services

import app.backend.CustomerService
import app.backend.types.customer.Customer
import shared.db.DynamoDBPrimitives
import zio._
import zio.dynamodb.DynamoDBQuery._
import zio.dynamodb.PartitionKeyExpression.PartitionKey
import zio.dynamodb.ProjectionExpression.$
import zio.dynamodb._
import zio.schema.{DeriveSchema, Schema}

import java.util.UUID

final case class DynamoDBCustomerService(executor: DynamoDBExecutor) extends CustomerService with DynamoDBPrimitives[Customer] {

  val tableResource = "ev-outlet-app.customer.table"
  val primaryKey    = "customerId"

  override val schema: Schema[Customer] = DeriveSchema.gen[Customer]

  override def register(customer: Customer): Task[Customer] =
    (for {
      insert <- ZIO.succeed(customer)
      _      <- put(tableResource, insert).execute
    } yield insert)
      .provideLayer(ZLayer.succeed(executor))

  override def getById(customerId: UUID): Task[Customer] =
    (for {
      result <- get[Customer](tableResource, PrimaryKey("customerId" -> customerId.toString)).execute
    } yield result)
      .flatMap {
        case Left(error)  => ZIO.fail(new Throwable(error))
        case Right(value) => ZIO.succeed(value)
      }
      .provideLayer(ZLayer.succeed(executor))

  override def getRfidTag(customerId: UUID): Task[String] =
    (for {
      result <- getByPK(customerId)
    } yield result.rfidTag)
      .provideLayer(ZLayer.succeed(executor))

  override def getCustomerByRfidTag(rfidTag: String): Task[Option[UUID]] =
    (for {
      query <- queryAll[Customer](tableResource)
                .indexName("rfidTag-on-customer-index")
                .whereKey(PartitionKey("rfidTag") === rfidTag)
                .execute

      result <- query.map(_.customerId).runHead

    } yield result)
      .provideLayer(ZLayer.succeed(executor))

  override def update(customerId: UUID, customer: Customer): Task[Option[Item]] =
    updateItem(tableResource, PrimaryKey("customerId" -> customerId.toString)) {
      $("address").set(customer.address) +
        $("email").set(customer.email) +
        $("paymentMethod").set(customer.paymentMethod)
    }.execute
      .provideLayer(ZLayer.succeed(executor))
}

object DynamoDBCustomerService {

  val live: ZLayer[DynamoDBExecutor, Nothing, CustomerService] =
    ZLayer.fromFunction(DynamoDBCustomerService.apply _)
}
