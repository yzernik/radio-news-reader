package services

import scala.concurrent.Future
import models.Station
import play.api.Play.current
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.Cursor
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID

object StationDAO {
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  // mongo stuff
  import play.api.Play.current
  lazy val db: reactivemongo.api.DB = ReactiveMongoPlugin.db
  def stationsColl: JSONCollection = db.collection[JSONCollection]("stations")

  /*
   * save a station.
   */
  def save(station: Station): Future[Station] = {
    stationsColl.insert(station).map(lastError =>
      station)
  }

  /*
   * find all stations.
   */
  def findAll(): Future[List[Station]] = {
    // let's do our query
    val cursor: Cursor[Station] = stationsColl.
      // find all stations
      find(Json.obj()).
      // sort them by creation date
      sort(Json.obj("created" -> -1)).
      // perform the query and get a cursor of JsObject
      cursor[Station]

    // gather all the JsObjects in a list
    cursor.collect[List]()
  }

  /*
   * find a station by id if it exists.
   */
  def find(id: String): Future[Option[Station]] = {
    // let's do our query
    val cursor: Cursor[Station] = stationsColl.
      // find all stations with matching id
      find(Json.obj("_id" -> Json.obj("$oid" -> id))).
      // perform the query and get a cursor of JsObject
      cursor[Station]

    // gather all the JsObjects in a list
    cursor.headOption
  }

}