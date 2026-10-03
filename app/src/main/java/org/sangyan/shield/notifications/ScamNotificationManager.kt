package org.sangyan.shield.notifications
import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import org.sangyan.shield.MainActivity
import org.sangyan.shield.R
import org.sangyan.shield.domain.model.Analysis
import org.sangyan.shield.privacy.SensitiveDataMasker
class ScamNotificationManager(private val context:Context) {
 fun show(result:Analysis) {
  val manager=context.getSystemService(NotificationManager::class.java)
  manager.createNotificationChannel(NotificationChannel("scam_alerts","SANGYAN Scam Alerts",NotificationManager.IMPORTANCE_HIGH).apply { description="Warnings from on-device SMS analysis"; lockscreenVisibility=android.app.Notification.VISIBILITY_PRIVATE })
  if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return
  val id=(result.timestamp % Int.MAX_VALUE).toInt()
  val intent=Intent(context,MainActivity::class.java).putExtra("analysis",Gson().toJson(SensitiveDataMasker.forStorage(result))).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
  val pending=PendingIntent.getActivity(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val reasons=result.findings.map { it.category }.distinct().take(3).joinToString(" + ")
  val notification=NotificationCompat.Builder(context,"scam_alerts").setSmallIcon(R.drawable.ic_shield).setContentTitle("Possible financial scam")
   .setContentText("${result.score}/100 Red Flag Score · $reasons").setStyle(NotificationCompat.BigTextStyle().bigText("${result.score}/100 Red Flag Score\n$reasons\nOpen to review the warning signs."))
   .setContentIntent(pending).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
  try { NotificationManagerCompat.from(context).notify(id,notification) } catch(_:SecurityException) { /* Permission may be revoked between checks. */ }
 }
}
