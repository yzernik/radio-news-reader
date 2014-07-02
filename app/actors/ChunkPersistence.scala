package actors

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import models.AudioChunk
import models.AudioChunkInfo
import services.AudioChunkInfoDAO

object ChunkPersistence {
  def props() =
    Props(classOf[ChunkPersistence])

  sealed trait ChunkPersistenceEvent
  case class NewChunk(chunk: AudioChunk, chunkInfo: AudioChunkInfo) extends ChunkPersistenceEvent

  /*
   * save audio bytes locally for now.
   */
  def saveBytes(bytes: Array[Byte]): String = {
    def uuid = java.util.UUID.randomUUID.toString
    val filename = "/tmp/" + "file_" + uuid + ".mp3"
    Files.write(Paths.get(filename), bytes, StandardOpenOption.CREATE)
    filename
  }

}

class ChunkPersistence extends Actor with ActorLogging {
  import ChunkPersistence.NewChunk

  def receive = {
    case NewChunk(chunk, chunkInfo) => {
      val loc = ChunkPersistence.saveBytes(chunk.audio.compact.toArray)
      val newChunkInfo = chunkInfo.copy(location = Some(loc))
      AudioChunkInfoDAO.save(newChunkInfo)
    }
  }

}