
import com.eds.dora.util.SysEnv
import play.api.{Application, GlobalSettings}

//This class is used by play framework
//Do not move it! it must be in default package.
object Global extends GlobalSettings {

  override def onStart(app: Application): Unit = {
    SysEnv.initZkDirs()
  }

  override def onStop(app: Application) {
  }
}