package actors

import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import models.AudioChunkInfo
import models.Station
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.Json
import services.StationDAO

object StationManager {
  def props(channel: Channel[AudioChunkInfo]) =
    Props(classOf[StationManager], channel)

  sealed trait StationManagerEvent
  case class ErrorEvent(error: String) extends StationManagerEvent
  case class CreateStationConnection(station: Station) extends StationManagerEvent
  case class UpdateStationConnections() extends StationManagerEvent
}

class StationManager(channel: Channel[AudioChunkInfo]) extends Actor with ActorLogging {
  import StationManager.ErrorEvent
  import StationManager.CreateStationConnection
  import StationManager.UpdateStationConnections

  // data structure holding the station connection actor refs.
  private var connections: Map[String, ActorRef] = Map.empty

  // update station connections on actor startup.
  self ! UpdateStationConnections()

  def receive = {

    case CreateStationConnection(station) => {
      log.info("received create station message.")
      createStationConnection(station)
    }

    case UpdateStationConnections() => {
      val futureStations = StationDAO.findAll()
      futureStations.map { stations =>
        log.info(Json.toJson(stations).toString)
        for (station <- stations) {
          createStationConnection(station)
        }
      }
    }

  }

  def createStationConnection(station: Station) = {
    val id: String = station._id.stringify

    if (!connections.contains(id)) {
      val sc = context.actorOf(StationListener.props(station))
      connections += ((id, sc))
      log.info("Station Connection: " + station.name + " created.")
      sc ! StationListener.RegisterChannel(channel)
    }
  }

}