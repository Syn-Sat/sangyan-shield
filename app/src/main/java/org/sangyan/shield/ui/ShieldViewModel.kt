package org.sangyan.shield.ui
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import org.sangyan.shield.ShieldApp
import org.sangyan.shield.domain.model.Analysis
class ShieldViewModel(application:Application):AndroidViewModel(application) {
 private val app=application as ShieldApp
 val settings=app.preferences.state
 val history=app.repository.history.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val result=MutableStateFlow<Analysis?>(null)
 val input=MutableStateFlow("")
 val busy=MutableStateFlow(false)
 val error=MutableStateFlow<String?>(null)
 fun update(transform:(org.sangyan.shield.data.repository.Settings)->org.sangyan.shield.data.repository.Settings) { app.preferences.update(transform(settings.value)) }
 fun history(enabled:Boolean) { viewModelScope.launch { app.repository.setHistory(enabled) } }
 fun clear() { viewModelScope.launch { app.repository.clear() } }
 fun analyze(source:String="Manual") { if(input.value.isBlank() || busy.value) return; val text=input.value; busy.value=true; error.value=null; viewModelScope.launch {
  try { result.value=app.repository.analyze(text,source) } catch(_:Exception) { error.value="Analysis could not complete. Please try again." } finally { busy.value=false }
 } }
 fun ocr(uri:Uri) {
  busy.value=true; error.value=null; input.value=""; result.value=null
  viewModelScope.launch {
   val latin=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
   val devanagari=TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
   try {
    val image=withContext(Dispatchers.IO) { InputImage.fromFilePath(app,uri) }
    val first=latin.process(image).await().text
    val second=if(settings.value.hindi) devanagari.process(image).await().text else ""
    input.value=(if(second.any { it in '\u0900'..'\u097f' }) second else first).take(20000)
    if(input.value.isBlank()) error.value="No text found. Choose a sharper screenshot with readable text."
   } catch(_:Exception) { error.value="Could not read this image. Try a smaller JPG or PNG screenshot." }
   finally { latin.close(); devanagari.close(); busy.value=false }
  }
 }
}
