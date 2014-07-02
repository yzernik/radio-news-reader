package models

import org.joda.time.DateTime

import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._

case class AudioChunkInfo(
  _id: BSONObjectID,
  time: DateTime,
  stationId: BSONObjectID,
  text: String,
  location: Option[String])

/*
 * Companion object for the station case class
*/
object AudioChunkInfo {
  import play.api.libs.concurrent.Execution.Implicits._
  import play.api.libs.json.Json
  import play.api.data._
  import play.api.data.Forms._

  // create the formats object for AudioChunkInfo.
  implicit val audioChunkInfoFormat = Json.format[AudioChunkInfo]

}