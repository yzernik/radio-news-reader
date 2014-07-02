package actors

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

import org.joda.time.DateTime

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import audio.FlacConverter
import models.AudioChunkInfo
import models.Station
import play.api.libs.iteratee.Concurrent.Channel
import reactivemongo.bson.BSONObjectID

object StationListener {
  def props(station: Station) =
    Props(classOf[StationListener], station)

  sealed trait StationListenerEvent
  case class ErrorEvent(error: String) extends StationListenerEvent
  case class RegisterChannel(chunkInfoChannel: Channel[AudioChunkInfo]) extends StationListenerEvent
}

class StationListener(station: Station) extends Actor with ActorLogging {
  import StationListener.RegisterChannel

  private var channels = Set.empty[Channel[AudioChunkInfo]]

  val conn = context.actorOf(StationConnection.props(station))
  val persist = context.actorOf(ChunkPersistence.props())

  def receive = {

    case StationConnection.NewAudioChunk(chunk) => {

      // create a flac converter object
      val fc = new FlacConverter()

      //convert audio chunk to text
      val futureChunkInfo = for {
        flacBytes <- Future(fc.getFlacBytes(chunk.audio.toArray))
        text <- google.Speech.getText(flacBytes)
      } yield {
        log.info("{}: {}", station.name, text)
        val time = DateTime.now()
        AudioChunkInfo(BSONObjectID.generate, chunk.time, station._id, text, None)
      }

      // send audio chunks to listening channels
      futureChunkInfo.onComplete {
        case Success(chunkInfo) => {
          // send to listening channels
          for (channel <- channels) {
            log.debug("pushing chunk to channel.")
            channel.push(chunkInfo)
          }

          // send to persistence
          persist ! ChunkPersistence.NewChunk(chunk, chunkInfo)
        }
        case _ =>
      }

    }

    case RegisterChannel(channel) => {
      channels += channel
      log.debug("new num channels: {}", channels.size)
    }

  }

}