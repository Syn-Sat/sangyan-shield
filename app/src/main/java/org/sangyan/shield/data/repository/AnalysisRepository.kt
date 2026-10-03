package org.sangyan.shield.data.repository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.sangyan.shield.domain.detector.ScamDetector
import org.sangyan.shield.domain.model.Analysis
import org.sangyan.shield.data.local.*
import org.sangyan.shield.privacy.SensitiveDataMasker
class AnalysisRepository(val detector:ScamDetector,val preferences:Preferences,private val dao:HistoryDao) {
 val history=dao.observe()
 private val lock=Mutex()
 suspend fun analyze(text:String,source:String="Manual"):Analysis=withContext(Dispatchers.Default) {
  val result=detector.analyze(text,preferences.state.value.hindi,source)
  lock.withLock { if(preferences.state.value.history) {
   val clean=SensitiveDataMasker.forStorage(result)
   dao.insert(HistoryEntry(timestamp=clean.timestamp,score=clean.score,level=clean.level,preview=clean.findings.map { it.category }.distinct().joinToString(" · ").ifEmpty { "No major warning signs" },resultJson=Gson().toJson(clean)))
   dao.trim()
  } }
  result
 }
 suspend fun clear()=lock.withLock { dao.clear() }
 suspend fun setHistory(enabled:Boolean)=lock.withLock { preferences.update(preferences.state.value.copy(history=enabled)) }
}
