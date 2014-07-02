package models

import play.modules.reactivemongo.json.BSONFormats._

import reactivemongo.bson.BSONObjectID

/*
 * Station case class
 */
case class Station(
  _id: BSONObjectID,
  name: String,
  url: String)

/*
 * Companion object for the station case class
*/
object Station {
  import play.api.libs.concurrent.Execution.Implicits._
  import play.api.libs.json.Json
  import play.api.data._
  import play.api.data.Forms._

  // create the formats object for Station.
  implicit val stationFormat = Json.format[Station]

  def save(station: Station) = {
    println(station)
  }

}