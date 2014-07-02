package actors

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

import org.joda.time.DateTime

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.util.ByteString
import akka.util.ByteStringBuilder
import models.AudioChunk
import models.Station
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Iteratee
import play.api.libs.ws.WS

object StationConnection {
  def props(station: Station) =
    Props(classOf[StationConnection], station)

  sealed trait StationConnectionEvent
  case object Tick extends StationConnectionEvent
  case class ErrorEvent(error: String) extends StationConnectionEvent
  case class NewAudioChunk(audio: AudioChunk) extends StationConnectionEvent
}

class StationConnection(station: Station) extends Actor with ActorLogging {
  import StationConnection.NewAudioChunk
  import StationConnection.Tick

  var buffer = new ByteStringBuilder()

  /*
   *  Iteratee for pushing into the audio stream channel.
   */
  val audioPushIteratee = Iteratee.foreach[Array[Byte]](byteArray =>
    {
      val bytes = ByteString(byteArray)
      buffer.append(bytes)
    })

  WS.url(station.url).withRequestTimeout(-1).get(headers =>
    audioPushIteratee)

  /*
   * This will schedule to send the Tick-message
   * to the actor after 0 seconds repeating every 10 seconds
   */
  val ticker = context.system.scheduler.schedule(0 seconds, 10 seconds, self, Tick)

  def receive = {
    case Tick => {
      log.debug("sending audio bytes.")
      val audioChunk = AudioChunk(DateTime.now(), station._id, buffer.result)
      context.parent ! NewAudioChunk(audioChunk)
      buffer = new ByteStringBuilder()
    }
  }

}