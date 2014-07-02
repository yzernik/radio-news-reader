import play.api.GlobalSettings
import actors.StationActors

object Global extends GlobalSettings {

  override def onStart(application: play.api.Application) {
    StationActors
  }

  override def onStop(application: play.api.Application) {
    // Maybe only needed if using separate actor system ?
    //StationStarter.system.shutdown()
  }
}