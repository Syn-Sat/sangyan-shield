package org.sangyan.shield.receiver
import android.content.*
import android.provider.Telephony
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.sangyan.shield.ShieldApp
import org.sangyan.shield.notifications.ScamNotificationManager
class SmsReceiver:BroadcastReceiver() {
 override fun onReceive(context:Context,intent:Intent) {
  if(intent.action!=Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
  val app=context.applicationContext as ShieldApp
  if(!app.preferences.state.value.live) return
  val parts=runCatching { Telephony.Sms.Intents.getMessagesFromIntent(intent) }.getOrNull() ?: return
  if(parts.isEmpty()) return
  val body=parts.joinToString("") { it.messageBody.orEmpty() }.take(20000)
  // Sender is available for routing only and is deliberately not persisted.
  val sender=parts.first().originatingAddress.orEmpty()
  val timestamp=parts.first().timestampMillis
  val pending=goAsync()
  app.scope.launch {
   try { withTimeout(8000) {
    if(app.preferences.state.value.live) {
     val result=app.repository.analyze(body,"SMS").copy(timestamp=timestamp)
     if(app.preferences.state.value.live && result.score>=app.preferences.state.value.threshold) ScamNotificationManager(context).show(result)
    }
   } } catch(_:Exception) { /* Never log message content or sender. */ } finally { pending.finish() }
  }
 }
}
