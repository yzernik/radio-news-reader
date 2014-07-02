package controllers

import scala.concurrent.Future
import actors.StationActors
import actors.StationManager
import akka.actor.actorRef2Scala
import models.AudioChunk
import models.Station
import play.api.libs.iteratee.Concurrent
import play.api.libs.iteratee.Enumeratee
import play.api.libs.json.JsError
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.__
import play.api.mvc.Action
import play.api.mvc.BodyParsers
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.Cursor
import reactivemongo.bson.BSONObjectID
import services.StationDAO
import play.api.libs.json.JsValue
import play.api.libs.EventSource
import play.api.libs.json.JsValue
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import models.AudioChunkInfo
import services.AudioChunkInfoDAO
import java.io.File

object Application extends Controller with MongoController {

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  /* Central hub for distributing chunks */
  val (chunkOut, chunkChannel) = Concurrent.broadcast[AudioChunk]

  /* Central hub for distributing chunkinfos */
  val (chunkInfoOut, chunkInfoChannel) = Concurrent.broadcast[AudioChunkInfo]

  /*
   * Index page.
   */
  def index = Action {
    Ok(views.html.index())
  }

  /*
   * REST endpoint for posting a new radio station.
   */
  def saveStation = Action.async(BodyParsers.parse.json) { request =>
    val addObjectId = __.json.update((__ \ '_id).json.put(Json.toJson(BSONObjectID.generate)))
    val jsonWithId = request.body.transform(addObjectId).get
    val placeResult = jsonWithId.validate[Station]
    placeResult.fold(
      errors => {
        Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
      },
      station => {
        val futureStation = StationDAO.save(station)
        futureStation.map { stn =>
          StationActors.stationManager ! StationManager.UpdateStationConnections()
          Created
        }
      })
  }

  /*
   * REST endpoint for fetching a single station.
   */
  def station(id: String) = Action.async {
    val futureStation = StationDAO.find(id)
    // reply with serialized JSON of the station.
    futureStation.map { station =>
      Ok(Json.toJson(station))
    }
  }

  /*
   * REST endpoint for fetching the list of all stations.
   */
  def stations = Action.async {
    val futureStations = StationDAO.findAll()
    // reply with serialized JSON of stations.
    futureStations.map { stations =>
      Ok(Json.toJson(stations))
    }
  }

  /*
   * REST endpoint for fetching a single audio chunk.
   */
  def chunk(id: String) = Action.async {
    val futureChunk = AudioChunkInfoDAO.find(id)
    // reply with serialized JSON of the chunk.
    futureChunk.map { maybeChunk =>
      maybeChunk.map { chunk =>
        Ok(Json.toJson(chunk))
      }.getOrElse {
        NotFound
      }
    }
  }

  /*
   * REST endpoint for fetching a single audio chunk audio.
   */
  def chunkStream(id: String) = Action.async {
    val futureChunk = AudioChunkInfoDAO.find(id)
    futureChunk.map { maybeChunk =>
      maybeChunk.map { chunk =>
        Ok.sendFile(
          content = new java.io.File(chunk.location.get),
          fileName = _ => chunk._id.stringify + ".mp3")
          .as("audio/mpeg")
      }.getOrElse {
        NotFound
      }
    }
  }

  /*
   * REST endpoint returning the audio stream of a station by id.
   */
  def listenToStation(id: String) = Action.async {
    def onlyThisStation(station: Station) = Enumeratee.filter[AudioChunk](chunk => chunk.stationId == station._id)
    val futureMaybeStation = StationDAO.find(id)
    futureMaybeStation.map { maybeStation =>
      maybeStation match {
        case Some(station) => {
          val toBytes: Enumeratee[AudioChunk, Array[Byte]] = Enumeratee.map[AudioChunk] { chunkInfo => chunkInfo.audio.compact.toArray }
          Ok.chunked(chunkOut
            through onlyThisStation(station)
            through toBytes).as("audio/mpeg")
        }
        case _ => BadRequest
      }
    }
  }

  /*
   *  Enumeratee for detecting disconnect of SSE stream 
   */
  def connDeathWatch(addr: String): Enumeratee[JsValue, JsValue] =
    Enumeratee.onIterateeDone { () => println(addr + " - SSE disconnected") }

  /*
   * SSE with transcribed text.
   */
  def chatFeed(id: String) = Action.async { req =>
    def onlyThisStation(station: Station) = Enumeratee.filter[AudioChunkInfo](chunk => chunk.stationId == station._id)
    val futureMaybeStation = StationDAO.find(id)
    futureMaybeStation.map { maybeStation =>
      maybeStation match {
        case Some(station) => {
          println(req.remoteAddress + " - SSE connected")
          val toText: Enumeratee[AudioChunkInfo, JsValue] = Enumeratee.map[AudioChunkInfo] { chunkInfo =>
            Json.toJson(chunkInfo)
          }
          Ok.feed(chunkInfoOut
            &> onlyThisStation(station)
            &> toText
            &> Concurrent.buffer(50)
            &> connDeathWatch(req.remoteAddress)
            &> EventSource()).as("text/event-stream")
        }
        case _ => BadRequest
      }
    }
  }

}