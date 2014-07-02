package models

import org.joda.time.DateTime
import akka.util.ByteString
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._

case class AudioChunk(
  time: DateTime,
  stationId: BSONObjectID,
  audio: ByteString)
