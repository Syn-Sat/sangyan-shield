package org.sangyan.shield.data.repository
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
data class Settings(val live:Boolean=false,val history:Boolean=false,val hindi:Boolean=true,val threshold:Int=70)
class Preferences(context:Context) {
 private val prefs=context.getSharedPreferences("privacy",Context.MODE_PRIVATE)
 private fun read()=Settings(prefs.getBoolean("live",false),prefs.getBoolean("history",false),prefs.getBoolean("hindi",true),prefs.getInt("threshold",70))
 private val mutable=MutableStateFlow(read())
 val state=mutable.asStateFlow()
 fun update(settings:Settings) {
  prefs.edit().putBoolean("live",settings.live).putBoolean("history",settings.history).putBoolean("hindi",settings.hindi).putInt("threshold",settings.threshold).apply()
  mutable.value=settings
 }
}
