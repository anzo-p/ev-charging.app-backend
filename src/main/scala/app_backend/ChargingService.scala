package app_backend

import app_backend.types.chargingSession.ChargingSession
import shared.types.chargingEvent.ChargingEvent
import zio.Task

import java.util.UUID

trait ChargingService {
  def getHistory(customerId: UUID): Task[List[ChargingSession]]

  def initialize(session: ChargingSession): Task[UUID]

  def get(sessionId: UUID): Task[ChargingSession]

  def updateSession(status: ChargingEvent): Task[Unit]

  def setStopRequested(sessionId: UUID): Task[Unit]
}
