package services

import scala.concurrent.Future
import models.Station
import play.api.Play.current
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.Cursor
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import models.AudioChunkInfo

object AudioChunkInfoDAO {
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  // mongo stuff
  import play.api.Play.current
  lazy val db: reactivemongo.api.DB = ReactiveMongoPlugin.db
  def chunksColl: JSONCollection = db.collection[JSONCollection]("chunks")

  /*
   * save a chunk.
   */
  def save(chunk: AudioChunkInfo): Future[AudioChunkInfo] = {
    chunksColl.insert(chunk).map(lastError =>
      chunk)
  }

  /*
   * find a chunk by id if it exists.
   */
  def find(id: String): Future[Option[AudioChunkInfo]] = {
    // let's do our query
    val cursor: Cursor[AudioChunkInfo] = chunksColl.
      // find all chunks with matching id
      find(Json.obj("_id" -> Json.obj("$oid" -> id))).
      // perform the query and get a cursor of JsObject
      cursor[AudioChunkInfo]

    // gather all the JsObjects in a list
    cursor.headOption
  }

  /*
   * find all chunks for a station by id.
   */
  def findByStationId(id: String): Future[List[AudioChunkInfo]] = {
    // let's do our query
    val cursor: Cursor[AudioChunkInfo] = chunksColl.
      // find all stations
      find(Json.obj("stationId" -> Json.obj("$oid" -> id))).
      // sort them by creation date
      sort(Json.obj("created" -> -1)).
      // perform the query and get a cursor of JsObject
      cursor[AudioChunkInfo]

    // gather all the JsObjects in a list
    cursor.collect[List]()
  }

}