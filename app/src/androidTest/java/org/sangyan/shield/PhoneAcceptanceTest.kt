package org.sangyan.shield

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.sangyan.shield.data.local.ShieldDatabase
import org.sangyan.shield.data.repository.AnalysisRepository
import org.sangyan.shield.data.repository.Preferences
import org.sangyan.shield.notifications.ScamNotificationManager
import org.sangyan.shield.ui.ShieldViewModel
import org.sangyan.shield.ui.demoMessages
import java.io.File

@RunWith(AndroidJUnit4::class)
class PhoneAcceptanceTest {
 @get:Rule val compose = createAndroidComposeRule<MainActivity>()
 private val app get() = compose.activity.application as ShieldApp
 private val vm get() = ViewModelProvider(compose.activity)[ShieldViewModel::class.java]
 private fun waitAnalysis() = compose.waitUntil(15000) { !vm.busy.value && vm.result.value != null }

 @Test fun allSevenDemoButtonsRenderExpectedBands() {
  compose.onNodeWithText("Try the live demo").performScrollTo().performClick()
  for (demo in demoMessages) {
   compose.onNodeWithTag("demo-${demo.title}").performScrollTo().performClick()
   waitAnalysis()
   val result=vm.result.value!!
   if(demo.expected=="Low") assertTrue(demo.title,result.score<25) else assertTrue(demo.title,result.score>=50)
   compose.onNodeWithContentDescription("Red Flag Score ${result.score} out of 100, ${result.level}").assertExists()
   println("PHONE_DEMO ${demo.title}: ${result.score} / ${result.level}")
   compose.onNodeWithContentDescription("Back").performClick()
  }
 }

 @Test fun manualEntryAnalyzesCredentialTheft() {
  compose.onNodeWithText("Analyze Message").performScrollTo().performClick()
  compose.onNodeWithTag("message-input").performTextReplacement("Send your OTP to verify your account")
  compose.onNodeWithTag("analyze-submit").performScrollTo().performClick()
  waitAnalysis()
  assertEquals(80,vm.result.value!!.score)
  compose.onNodeWithText("SEVERE WARNING").assertExists()
 }

 @Test fun urlCheckerHandlesOfficialAndImitationDomains() {
  compose.onNodeWithText("Check URL").performScrollTo().performClick()
  compose.onNodeWithTag("message-input").performTextReplacement("https://www.sebi.gov.in")
  compose.onNodeWithTag("analyze-submit").performScrollTo().performClick()
  waitAnalysis()
  assertTrue(vm.result.value!!.score<25)
  compose.onNodeWithContentDescription("Back").performClick()
  compose.onNodeWithTag("message-input").performTextReplacement("https://nsdl-secure-kyc.xyz")
  compose.onNodeWithTag("analyze-submit").performScrollTo().performClick()
  waitAnalysis()
  assertTrue(vm.result.value!!.findings.any { it.category=="Possible NSDL impersonation" })
 }

 private fun scanFixture(lines:List<String>):String {
  val file=File(app.cacheDir,"shield-acceptance-ocr.png")
  val bitmap=Bitmap.createBitmap(1500,650,Bitmap.Config.ARGB_8888)
  Canvas(bitmap).apply {
   drawColor(Color.WHITE)
   val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.BLACK; textSize=62f }
   lines.forEachIndexed { index,line -> drawText(line,45f,100f+index*100f,paint) }
  }
  file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
  bitmap.recycle()
  try {
   compose.runOnUiThread { vm.ocr(Uri.fromFile(file)) }
   compose.waitUntil(90000) { !vm.busy.value }
   assertNull("OCR error",vm.error.value)
   assertTrue("OCR should produce text",vm.input.value.isNotBlank())
   val text=vm.input.value
   compose.runOnUiThread { vm.analyze("Screenshot") }
   waitAnalysis()
   assertTrue("OCR text should yield severe warning: $text",vm.result.value!!.score>=75)
   return text
  } finally { file.delete() }
 }
 @Test fun bundledLatinOcrRunsOnPhone() {
  val text=scanFixture(listOf("Install AnyDesk so our support", "executive can fix your trading account."))
  assertTrue(text.contains("AnyDesk",true))
 }
 @Test fun bundledDevanagariOcrRunsOnPhone() {
  val text=scanFixture(listOf("OTP शेयर करें", "पैसा डबल", "गारंटीड रिटर्न"))
  assertTrue(text.any { it in '\u0900'..'\u097f' })
 }

 @Test fun roomHistoryRedactsAndDisablesAndDeletes() = runBlocking {
  val isolated=object:ContextWrapper(app) {
   override fun getSharedPreferences(name:String,mode:Int)=super.getSharedPreferences("acceptance-test-$name",mode)
  }
  val prefs=Preferences(isolated)
  val db=Room.inMemoryDatabaseBuilder(app,ShieldDatabase::class.java).build()
  try {
   val repository=AnalysisRepository(app.repository.detector,prefs,db.history())
   repository.setHistory(true)
   repository.analyze("Send OTP 482921 and password SecretFixture! at https://bad.xyz?pin=1234")
   val records=db.history().observe().first()
   assertEquals(1,records.size)
   val saved=records.single().resultJson
   listOf("482921","SecretFixture","1234","bad.xyz").forEach { assertFalse("Stored sensitive fixture: $it",saved.contains(it)) }
   repository.setHistory(false)
   repository.analyze("Guaranteed profit")
   assertEquals(1,db.history().observe().first().size)
   repository.clear()
   assertTrue(db.history().observe().first().isEmpty())
  } finally { db.close(); isolated.getSharedPreferences("privacy",Context.MODE_PRIVATE).edit().clear().commit() }
 }

 @Test fun notificationAppearsAndTapOpensSanitizedDetails() {
  assumeTrue("Notification consent is required",ContextCompat.checkSelfPermission(app,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)
  val result=app.repository.detector.analyze("Install AnyDesk so our support executive can fix your trading account.",source="SMS")
  val manager=app.getSystemService(NotificationManager::class.java)
  val notificationId=(result.timestamp%Int.MAX_VALUE).toInt()
  try {
   ScamNotificationManager(app).show(result)
   compose.waitUntil(10000) { manager.activeNotifications.any { it.id==notificationId } }
   val notification=manager.activeNotifications.first { it.id==notificationId }
   assertTrue(notification.notification.extras.getCharSequence("android.text").toString().contains("80/100"))
   val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
   device.openNotification()
   device.waitForIdle(3000)
   var clicked=false
   for(attempt in 1..3) {
    try {
     val card=device.wait(Until.findObject(By.text("Possible financial scam")),10000)
     assertNotNull("SANGYAN notification should be visible",card)
     card.click()
     clicked=true
     break
    } catch(_:StaleObjectException) { device.waitForIdle(3000) }
   }
   assertTrue("Notification tap should complete",clicked)
   compose.waitUntil(15000) { vm.result.value?.source=="SMS" }
   assertEquals(80,vm.result.value!!.score)
   assertTrue(vm.result.value!!.findings.all { it.evidence=="[Evidence omitted from saved history]" })
   compose.onNodeWithText("SEVERE WARNING").assertExists()
  } finally { manager.cancel(notificationId) }
 }
}
