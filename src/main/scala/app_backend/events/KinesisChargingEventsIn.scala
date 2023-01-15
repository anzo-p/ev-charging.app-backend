package app_backend.events

import app_backend.types.chargingSession.ChargingSession
import app_backend.{ChargingService, CustomerService}
import shared.events.{ChargingEventConsumer, ChargingEventProducer, DeadLetterProducer}
import shared.types.chargingEvent.ChargingEvent
import shared.types.enums.{EventInitiator, OutletDeviceState}
import zio._

final case class KinesisChargingEventsIn(
    customerService: CustomerService,
    chargingService: ChargingService,
    toBackend: ChargingEventProducer,
    deadLetters: DeadLetterProducer
  ) extends ChargingEventConsumer {

  val applicationName: String = "app-backend"

  def follow: EventInitiator = EventInitiator.OutletBackend

  def consume(data: ChargingEvent): Task[Unit] =
    data.outletState match {
      case OutletDeviceState.DeviceRequestsCharging =>
        for {
          customerId <- customerService.getCustomerIdByRfidTag(data.recentSession.rfidTag)
          session    <- ZIO.from(ChargingSession.fromChargingEvent(customerId.get, data).copy(outletState = OutletDeviceState.Charging))
          sessionId  <- chargingService.initialize(session)
          _          <- toBackend.put(session.copy(sessionId = sessionId).toChargingEvent)
          // else NACK
        } yield ()

      case OutletDeviceState.Charging | OutletDeviceState.ChargingFinished =>
        for {
          _ <- chargingService.updateSession(data)
        } yield ()

      case state =>
        for {
          _ <- ZIO.succeed(println(s"Something else $state"))
        } yield ()
    }
}

object KinesisChargingEventsIn {

  val live
      : ZLayer[CustomerService with ChargingService with ChargingEventProducer with DeadLetterProducer, Nothing, KinesisChargingEventsIn] =
    ZLayer.fromFunction(KinesisChargingEventsIn.apply _)
}

/*
  TODO
  should we nack somehow or should caller timeout?
  - or should we typefy the errors and handle more elegant than through Throwables?
    - Eg.
      - rfidToken was not found or valid
      - customer already has an active session
      - charging session was not found
      - charging was already started
      - charging was already finished
  - all said timeouts would be needed anyway
  - the device backend would have a whitelist of known good rfidTags to start/stop instantly and settle all else later
 */