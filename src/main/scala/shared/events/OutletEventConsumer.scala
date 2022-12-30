package shared.events

import nl.vroste.zio.kinesis.client.Record
import nl.vroste.zio.kinesis.client.zionative.{Consumer, LeaseRepository}
import shared.types.enums.OutletStateRequester
import shared.types.outletStatus.{OutletStatusEvent, OutletStatusEventSerDes}
import zio.Console.printLine
import zio.aws.kinesis.Kinesis
import zio.{durationInt, Task, ZIO}

trait OutletEventConsumer {

  val resourceName = "ev-outlet-app.outlet-events.stream"

  def applicationName: String

  def follow: OutletStateRequester

  def deadLetters: DeadLetterProducer

  def consume(record: Record[OutletStatusEvent]): Task[Unit]

  private def consumeAndCatch(record: Record[OutletStatusEvent]): ZIO[Any, Throwable, Unit] =
    consume(record).catchAll {
      case th: Throwable => deadLetters.send[OutletStatusEvent](record, th)
      case _ => ZIO.succeed(())
    }

  def start: ZIO[Kinesis with LeaseRepository with Any, Throwable, Unit] =
    Consumer
      .shardedStream(
        streamName      = resourceName,
        applicationName = s"ev-outlet-app.outlet-events-checkpoints.$applicationName.table",
        deserializer    = OutletStatusEventSerDes.byteArray
      )
      .flatMapPar(4) {
        case (shardId, shardStream, checkpointer) =>
          shardStream
            .filter(_.data.requester == follow)
            .tap(record => printLine(s"Processing record $record on shard $shardId"))
            .tap(_ => ZIO.succeed(Thread.sleep(1111))) // slow down for now, later find out why required
            .tap(consumeAndCatch)
            .tap(checkpointer.stage(_))
            .viaFunction(checkpointer.checkpointBatched[Any](nr = 1000, interval = 5.minutes))
      }
      .tap(_ => ZIO.succeed(Thread.sleep(1111))) // slow down for now, later find out why required
      .runDrain
}
