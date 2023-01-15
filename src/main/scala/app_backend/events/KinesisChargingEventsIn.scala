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

  def consume(event: ChargingEvent): Task[Unit] =
    event.outletState match {
      case OutletDeviceState.DeviceRequestsCharging =>
        for {
          _ <- customerService.getCustomerIdByRfidTag(event.recentSession.rfidTag).flatMap {
                case Some(customerId) =>
                  for {
                    session   <- ZIO.from(ChargingSession.fromChargingEvent(customerId, event).copy(outletState = OutletDeviceState.Charging))
                    sessionId <- chargingService.initialize(session)
                    _         <- toBackend.put(session.copy(sessionId = sessionId).toChargingEvent)
                  } yield ()

                case None =>
                  toBackend.put(
                    event.copy(
                      initiator   = EventInitiator.AppBackend,
                      outletState = OutletDeviceState.AppDeniesCharging
                    ))
              }
        } yield ()

      case OutletDeviceState.DeviceDeniesCharging =>
        for {
          sessionId <- ZIO.fromOption(event.recentSession.sessionId).orElseFail(new Error("[KinesisChargingEventsIn] no session id"))
          _         <- chargingService.setDeviceDenies(sessionId)
        } yield ()

      case OutletDeviceState.Charging | OutletDeviceState.ChargingFinished =>
        for {
          _ <- chargingService.updateSession(event)
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
