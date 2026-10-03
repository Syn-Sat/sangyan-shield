package org.sangyan.shield
import android.app.Application
import androidx.room.Room
import org.sangyan.shield.data.local.ShieldDatabase
import org.sangyan.shield.data.repository.*
import org.sangyan.shield.domain.detector.*
import kotlinx.coroutines.*
class ShieldApp:Application() {
 val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
 val preferences by lazy { Preferences(this) }
 val repository by lazy {
  val config=DetectorConfig { assets.open("config/$it").bufferedReader().use { r->r.readText() } }
  AnalysisRepository(ScamDetector(config),preferences,Room.databaseBuilder(this,ShieldDatabase::class.java,"shield.db").build().history())
 }
}
