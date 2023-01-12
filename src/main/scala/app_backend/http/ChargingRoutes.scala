package app_backend.http

import app_backend.http.dto.{ChargingSessionDto, CreateChargingSessionDto}
import app_backend.{ChargingService, CustomerService}
import shared.events.ChargingEventProducer
import shared.http.BaseRoutes
import shared.types.enums.OutletDeviceState
import shared.validation.InputValidation._
import zhttp.http._
import zio._
import zio.json.{DecoderOps, EncoderOps}

final case class ChargingRoutes(customerService: CustomerService, chargingService: ChargingService, toBackend: ChargingEventProducer)
    extends BaseRoutes {

  val routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case Method.GET -> !! / "chargers" / "customer" / id / "history" =>
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

      case req @ Method.POST -> !! / "chargers" / "start" =>
        (for {
          body <- req.body.asString.mapError(serverError)
          dto  <- body.fromJson[CreateChargingSessionDto].orFail(invalidPayload)
          // create <- CreateChargingSession.validate(dto).orFail(invalidPayload) - nothing to validate yet
          rfidTag <- customerService.getRfidTag(dto.customerId).orElseFail(invalidPayload("this customer doesn't exist"))
          session = dto.toModel.copy(rfidTag = rfidTag)
          _ <- chargingService.initialize(session).mapError(th => badRequest(th.getMessage))
          _ <- toBackend.put(session.toChargingEvent).mapError(serverError)
          // app will forward to poll for status reports
        } yield {
          Response(
            Status.Created,
            defaultHeaders,
            Body.fromString {
              ChargingSessionDto.fromModel(session).toJson
            }
          )
        }).respond

      case Method.GET -> !! / "chargers" / "session" / id =>
        (for {
          sessionId <- validateUUID(id, "session").toEither.orFail(unProcessableEntity)
          session   <- chargingService.getSession(sessionId).mapError(th => badRequest(th.getMessage))
        } yield {
          Response(
            Status.Ok,
            defaultHeaders,
            Body.fromString {
              ChargingSessionDto.fromModel(session).toJson
            }
          )
        }).respond

      case Method.GET -> !! / "chargers" / "session" / id / "stop" =>
        (for {
          sessionId <- validateUUID(id, "session").toEither.orFail(unProcessableEntity)
          session   <- chargingService.getSession(sessionId).mapError(th => badRequest(th.getMessage))
          _         <- chargingService.setStopRequested(session.sessionId).mapError(th => badRequest(th.getMessage))
          _         <- toBackend.put(session.toChargingEvent.copy(outletState = OutletDeviceState.AppRequestsStop)).mapError(serverError)
          // app will forward to poll for final report
        } yield {
          Response(Status.Ok, defaultHeaders)
        }).respond
    }
}

object ChargingRoutes {

  val live: ZLayer[CustomerService with ChargingService with ChargingEventProducer, Nothing, ChargingRoutes] =
    ZLayer.fromFunction(ChargingRoutes.apply _)
}
