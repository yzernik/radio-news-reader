package actors

import akka.actor.ActorSystem
import controllers.Application

object StationActors {

  /** stations actor system */
  val system = ActorSystem("stations")

  /** Supervisor for stations */
  val stationManager = system.actorOf(StationManager.props(Application.chunkInfoChannel), "StationManager")

}