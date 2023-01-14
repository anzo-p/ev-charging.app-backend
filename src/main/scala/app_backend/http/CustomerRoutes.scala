package app_backend.http

import app_backend.{ChargingService, CustomerService}
import app_backend.http.dto.{ChargingSessionDto, CreateCustomer}
import shared.http.BaseRoutes
import shared.validation.InputValidation.validateUUID
import zhttp.http._
import zio.ZLayer
import zio.json.{DecoderOps, EncoderOps}

final case class CustomerRoutes(customerService: CustomerService, chargingService: ChargingService) extends BaseRoutes {

  val routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case Method.GET -> !! / "api" / "customers" / customer =>
        (for {
          customerId <- validateUUID(customer, "customer").toEither.orFail(unProcessableEntity)
          him        <- customerService.getById(customerId).mapError(th => badRequest(th.getMessage))
        } yield {
          Response(
            Status.Ok,
            defaultHeaders,
            Body.fromString {
              CreateCustomer.fromModel(him).toJson
            }
          )
        }).respond

      case req @ Method.POST -> !! / "api" / "customers" =>
        (for {
          body <- req.body.asString.mapError(serverError)
          dto  <- body.fromJson[CreateCustomer].orFail(invalidPayload)
          //create <- CreateCustomer.validate(dto).orFail(invalidPayload)
          them <- customerService.register(dto.toModel).mapError(th => badRequest(th.getMessage))
        } yield {
          Response(
            Status.Created,
            defaultHeaders,
            Body.fromString {
              CreateCustomer.fromModel(them).toJson
            }
          )
        }).respond

      case Method.GET -> !! / "api" / "customers" / id / "charging-history" =>
        (for {
          customerId <- validateUUID(id, "customer").toEither.orFail(unProcessableEntity)
          history    <- chargingService.getHistory(customerId).mapError(th => badRequest(th.getMessage))
        } yield {
          Response(
            Status.Created,
            defaultHeaders,
            Body.fromString {
              history.map(ChargingSessionDto.fromModel).toJson
            }
          )
        }).respond
    }
}

object CustomerRoutes {

  val live: ZLayer[CustomerService with ChargingService, Nothing, CustomerRoutes] =
    ZLayer.fromFunction(CustomerRoutes.apply _)
}
