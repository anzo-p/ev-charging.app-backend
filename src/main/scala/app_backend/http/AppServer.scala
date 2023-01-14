package app_backend.http

import shared.http.HealthRoutes
import zhttp.http.HttpApp
import zhttp.service.Server
import zio.{ZIO, ZLayer}

final case class AppServer(customerRoutes: CustomerRoutes, chargingRequestRoutes: ChargingRoutes) {

  val allRoutes: HttpApp[Any, Throwable] =
    customerRoutes.routes ++ chargingRequestRoutes.routes ++ HealthRoutes.routes

  val start: ZIO[Any, Throwable, Nothing] =
    Server.start(8080, allRoutes)
}

object AppServer {

  val live: ZLayer[CustomerRoutes with ChargingRoutes, Nothing, AppServer] =
    ZLayer.fromFunction(AppServer.apply _)
}
