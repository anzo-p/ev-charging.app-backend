package app_backend.http

import app_backend.CustomerService
import app_backend.http.dto.CreateCustomer
import shared.http.BaseRoutes
import shared.validation.InputValidation.validateUUID
import zhttp.http._
import zio.ZLayer
import zio.json._

final case class CustomerRoutes(service: CustomerService) extends BaseRoutes {

  val routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case Method.GET -> !! / "customers" / customer =>
        (for {
          customerId <- validateUUID(customer, "customer").toEither.orFail(unProcessableEntity)
          him        <- service.getById(customerId).mapError(th => badRequest(th.getMessage))
        } yield {
          Response(
            Status.Ok,
            defaultHeaders,
            Body.fromString {
              CreateCustomer.fromModel(him).toJson
            }
          )
        }).respond

      case req @ Method.POST -> !! / "customers" =>
        (for {
          body <- req.body.asString.mapError(serverError)
          dto  <- body.fromJson[CreateCustomer].orFail(invalidPayload)
          //create <- CreateCustomer.validate(dto).orFail(invalidPayload)
          them <- service.register(dto.toModel).mapError(th => badRequest(th.getMessage))
        } yield {
          Response(
            Status.Created,
            defaultHeaders,
            Body.fromString {
              CreateCustomer.fromModel(them).toJson
            }
          )
        }).respond
    }
}

object CustomerRoutes {

  val live: ZLayer[CustomerService, Nothing, CustomerRoutes] =
    ZLayer.fromFunction(CustomerRoutes.apply _)
}
