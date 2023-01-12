package app_backend

import app_backend.types.chargingSession.ChargingSession
import shared.types.chargingEvent.ChargingEvent
import zio.Task

import java.util.UUID

trait ChargingService {
  def getHistory(customerId: UUID): Task[List[ChargingSession]]

  def initialize(session: ChargingSession): Task[Unit]

  def getSession(sessionId: UUID): Task[ChargingSession]

  def aggregateSessionTotals(status: ChargingEvent): Task[Unit]

  def setStopRequested(sessionId: UUID): Task[Unit]
}
