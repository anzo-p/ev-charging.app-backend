package outlet.backend.events

import nl.vroste.zio.kinesis.client.Record
import outlet.backend.ChargerOutletService
import shared.events.{DeadLetterProducer, OutletEventConsumer, OutletEventProducer}
import shared.types.enums.{OutletDeviceState, OutletStateRequester}
import shared.types.outletStatus.OutletStatusEvent
import zio._

final case class DeviceEndOutletEventConsumer(
    outletService: ChargerOutletService,
    correspondent: OutletEventProducer,
    deadLetters: DeadLetterProducer
  ) extends OutletEventConsumer {

  val applicationName: String = "outlet-backend"

  def follow: OutletStateRequester = OutletStateRequester.Application

  def consume(record: Record[OutletStatusEvent]): Task[Unit] =
    record.data.outletState match {
      case OutletDeviceState.ChargingRequested =>
        for {
          _ <- ZIO.succeed(println("ChargingRequested")).unit
          _ <- outletService.setCharging(record.data.outletId, record.data.recentSession.rfidTag)
          // else NACK back
        } yield ()

      case OutletDeviceState.Charging =>
        for {
          _ <- ZIO.succeed(println("App has ACKed our Charging Request")).unit
          // http client to call back to aws api gateway websocket to start charging
          _ <- outletService.aggregateConsumption(record.data.copy(outletState = OutletDeviceState.Charging)).unit
        } yield ()

      case OutletDeviceState.StoppingRequested =>
        for {
          _           <- ZIO.succeed(println("StoppingRequested")).unit
          finalReport <- outletService.stopCharging(record.data)
          _           <- correspondent.put(finalReport.toOutletStatus.copy(outletState = OutletDeviceState.Finished))
        } yield ()

      case status =>
        for {
          _ <- ZIO.succeed(println(s"Something else $status")).unit
        } yield ()
    }
}

object DeviceEndOutletEventConsumer {

  val live: ZLayer[ChargerOutletService with OutletEventProducer with DeadLetterProducer, Nothing, DeviceEndOutletEventConsumer] =
    ZLayer.fromFunction(DeviceEndOutletEventConsumer.apply _)
}
