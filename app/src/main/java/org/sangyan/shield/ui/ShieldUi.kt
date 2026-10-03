package org.sangyan.shield.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.Gson
import org.sangyan.shield.domain.model.Analysis
import java.text.DateFormat
import java.util.Date

private const val SUPPORT_URL="https://grievance-clock.vercel.app/"

private val Teal=Color(0xFF076B61)
private val Mint=Color(0xFF95F4CE)
private val Navy=Color(0xFF102C36)
@Composable fun ShieldUi(vm:ShieldViewModel) {
 val dark=isSystemInDarkTheme()
 MaterialTheme(colorScheme=if(dark) darkColorScheme(primary=Mint,secondary=Color(0xFFAACCEB),surface=Color(0xFF10232B),background=Color(0xFF091A22)) else lightColorScheme(primary=Teal,secondary=Color(0xFF3D6379),surface=Color(0xFFF7FAF9),background=Color(0xFFEDF3F2))) {
  AppContent(vm)
 }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun AppContent(vm:ShieldViewModel) {
 val context=LocalContext.current
 fun openHelpWebsite() {
  try {
   context.startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse(SUPPORT_URL)))
  } catch(_:android.content.ActivityNotFoundException) {
   vm.error.value="No browser is available. Open $SUPPORT_URL in a browser to get help."
  }
 }
 val settings by vm.settings.collectAsStateWithLifecycle()
 val result by vm.result.collectAsStateWithLifecycle()
 val input by vm.input.collectAsStateWithLifecycle()
 val busy by vm.busy.collectAsStateWithLifecycle()
 val error by vm.error.collectAsStateWithLifecycle()
 val history by vm.history.collectAsStateWithLifecycle()
 var page by rememberSaveable { mutableStateOf("Home") }
 var consent by remember { mutableStateOf(false) }
 var clearConfirm by remember { mutableStateOf(false) }
 var smsGranted by remember { mutableStateOf(false) }
 var alertsGranted by remember { mutableStateOf(false) }
 fun refresh() {
  smsGranted=ContextCompat.checkSelfPermission(context,Manifest.permission.RECEIVE_SMS)==PackageManager.PERMISSION_GRANTED
  alertsGranted=NotificationManagerCompat.from(context).areNotificationsEnabled()
  if(!smsGranted && settings.live) vm.update { it.copy(live=false) }
 }
 val lifecycle=LocalLifecycleOwner.current
 DisposableEffect(lifecycle) { val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_RESUME) refresh() }; lifecycle.lifecycle.addObserver(observer); refresh(); onDispose { lifecycle.lifecycle.removeObserver(observer) } }
 val notifications=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
 val sms=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
  smsGranted=granted; vm.update { it.copy(live=granted) }
  if(granted && Build.VERSION.SDK_INT>=33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
  if(!granted) vm.error.value="SMS permission was not granted. Manual analysis and screenshot scanning remain available. Your installer may restrict SMS access; see the README."
 }
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if(uri!=null) vm.ocr(uri) }
 fun go(target:String) { vm.result.value=null; vm.error.value=null; if(target!=page) vm.input.value=""; page=target }
 fun requestLive(enabled:Boolean) { if(!enabled) vm.update { it.copy(live=false) } else consent=true }
 BackHandler(enabled=result!=null || page!="Home") { if(result!=null) vm.result.value=null else go("Home") }
 if(consent) AlertDialog(onDismissRequest={consent=false},icon={Icon(Icons.Outlined.Security,null)},title={Text("Enable Live SMS Protection?")},text={Text("Allow SANGYAN Shield to receive new SMS messages and analyze their text on this device. It does not read your existing inbox, upload messages, or save message bodies. SMS access is optional. Android will ask for permission next, then notification access for alerts.")},confirmButton={TextButton(onClick={consent=false;sms.launch(Manifest.permission.RECEIVE_SMS)}) { Text("Continue") }},dismissButton={TextButton(onClick={consent=false}) { Text("Not now") }})
 if(clearConfirm) AlertDialog(onDismissRequest={clearConfirm=false},title={Text("Delete all history?")},text={Text("Saved analysis summaries will be permanently deleted from this device.")},confirmButton={TextButton(onClick={vm.clear();clearConfirm=false}) { Text("Delete all") }},dismissButton={TextButton(onClick={clearConfirm=false}) { Text("Cancel") }})
 Scaffold(topBar={TopAppBar(title={Column { Text("SANGYAN Shield",fontWeight=FontWeight.Bold,fontSize=20.sp); Text("INVESTOR PROTECTION",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary) }},navigationIcon={if(page!="Home" || result!=null) IconButton(onClick={if(result!=null) vm.result.value=null else go("Home")}) { Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Back") } else Icon(Icons.Outlined.VerifiedUser,null,Modifier.padding(start=16.dp))},actions={IconButton(onClick={go("About / Privacy")}) { Icon(Icons.Outlined.Info,"About and privacy") }})},bottomBar={NavigationBar {
  listOf(Triple("Home",Icons.Outlined.Home,"Home"),Triple("Analyze Message",Icons.AutoMirrored.Outlined.Message,"Analyze"),Triple("History",Icons.Outlined.History,"History"),Triple("Settings",Icons.Outlined.Settings,"Settings")).forEach { (target,icon,label) -> NavigationBarItem(selected=page==target && result==null,onClick={go(target)},icon={Icon(icon,null)},label={Text(label)}) }
 }}) { padding ->
  Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal=20.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
   if(error!=null) Notice(error!!,Icons.Outlined.Info)
   if(result!=null) { Button(onClick={openHelpWebsite()},modifier=Modifier.fillMaxWidth()) { Text("Got scammed? Get help") }; ResultContent(result!!); OutlinedButton(onClick={vm.result.value=null},modifier=Modifier.fillMaxWidth()) { Text("Back to $page") } }
   else when(page) {
    "Home" -> {
     Card(colors=CardDefaults.cardColors(containerColor=Navy),shape=RoundedCornerShape(28.dp)) {
      Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Navy,Color(0xFF14594E)))).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
       Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) { Icon(Icons.Outlined.Shield,null,tint=Mint); Text("YOUR FINANCIAL SAFETY COMPANION",color=Mint,style=MaterialTheme.typography.labelSmall) }
       Text("Check before you\nclick, pay, or trust.",color=Color.White,fontSize=30.sp,lineHeight=36.sp,fontWeight=FontWeight.Bold)
       Text("Spot warning signs. Understand the risk. Stay in control.",color=Color(0xFFCBE4DF),style=MaterialTheme.typography.bodyLarge)
       FilledTonalButton(onClick={go("Demo Mode")}) { Icon(Icons.Outlined.PlayArrow,null); Spacer(Modifier.width(8.dp)); Text("Try the live demo") }
      }
     }
     Panel { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Live SMS Protection",fontWeight=FontWeight.Bold); Text(if(settings.live && smsGranted) "ON · Monitoring new SMS" else "OFF · Enable with your permission",style=MaterialTheme.typography.bodySmall) }; Switch(settings.live && smsGranted,onCheckedChange={requestLive(it)}) }; TextButton(onClick={go("Live Protection")}) { Text("Manage protection →") } }
     Action("Got scammed? Get help","Open the support website in your browser",Icons.Outlined.SupportAgent) { openHelpWebsite() }
     Text("Check something suspicious",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
     Action("Analyze Message","SMS, WhatsApp, Telegram or email",Icons.AutoMirrored.Outlined.Message) { go("Analyze Message") }
     Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
      SmallAction("Scan screenshot",Icons.Outlined.Image,Modifier.weight(1f)) { go("Screenshot Scan") }
      SmallAction("Check URL",Icons.Outlined.Link,Modifier.weight(1f)) { go("URL Checker") }
     }
     val recent=history.firstOrNull { it.score>=settings.threshold }
     if(recent!=null) Action("Recent alert · ${recent.score}/100",recent.preview,Icons.Outlined.WarningAmber) { vm.result.value=Gson().fromJson(recent.resultJson,Analysis::class.java) }
     Notice("Analysis happens on-device. Help opens an external website; no message text is sent automatically.",Icons.Outlined.Lock)
    }
    "Analyze Message","URL Checker","Screenshot Scan" -> {
     Text(page,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
     Text(when(page) { "URL Checker"->"Inspect a link without opening it. Redirects and website content are not fetched."; "Screenshot Scan"->"Choose a screenshot. Bundled OCR reads Latin and Devanagari text on your device. Review the extracted text before analysis."; else->"Paste a message to see its warning signs. We never open extracted links automatically." })
     if(page=="Screenshot Scan") Button(onClick={picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},enabled=!busy,modifier=Modifier.fillMaxWidth()) { Icon(Icons.Outlined.PhotoLibrary,null); Spacer(Modifier.width(8.dp)); Text("Choose screenshot") }
     OutlinedTextField(value=input,onValueChange={vm.input.value=it.take(20000)},label={Text(if(page=="URL Checker") "Paste URL" else if(page=="Screenshot Scan") "Review and edit extracted text" else "Paste SMS, WhatsApp, Telegram, or email text")},minLines=if(page=="URL Checker") 3 else 7,modifier=Modifier.fillMaxWidth().testTag("message-input"),supportingText={Text("${input.length}/20,000 · Kept in memory only")})
     Button(onClick={vm.analyze(if(page=="Screenshot Scan") "Screenshot" else if(page=="URL Checker") "URL" else "Manual")},enabled=input.isNotBlank()&&!busy,modifier=Modifier.fillMaxWidth().height(54.dp).testTag("analyze-submit")) { if(busy) CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp) else Icon(Icons.Outlined.Search,null); Spacer(Modifier.width(8.dp)); Text(if(busy) "Working on-device…" else "Analyze") }
     TextButton(onClick={vm.input.value=""}) { Text("Clear text") }
     Notice("Your messages are analyzed on your device. Sensitive credentials such as OTPs are not stored.",Icons.Outlined.PrivacyTip)
    }
    "Live Protection" -> {
     Text("Protection, on your terms",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
     Notice("New SMS → local analysis → Red Flag Score → private alert. Existing inbox messages are never read.",Icons.Outlined.Security)
     Panel { Toggle("Live SMS Protection",settings.live&&smsGranted) { requestLive(it) }; Text("SMS permission: ${if(smsGranted) "granted" else "not granted"}"); Text("Notifications: ${if(alertsGranted) "enabled" else "disabled"}"); Text("Alert threshold: ${settings.threshold}/100") }
     if(!alertsGranted) Button(onClick={if(Build.VERSION.SDK_INT>=33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS) else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))}) { Text("Enable notifications") }
     OutlinedButton(onClick={context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:${context.packageName}")))}) { Text("Open Android app settings") }
     Text("Sideload testing: SMS access depends on the installer, Android version and device policy. Launch the app after installation and grant permissions. Force-stopping the app prevents delivery until you open it again. RCS, WhatsApp and Telegram messages must be pasted or scanned manually.")
    }
    "History" -> {
     Text("Your analysis history",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
     Toggle("Save sanitized summaries",settings.history) { vm.history(it) }
     Text("Only scores, categories and organization names are saved. Message bodies, senders, credentials, and URL paths are omitted. Latest 100 results are kept.")
     if(history.isEmpty()) Notice("Nothing saved yet. History is optional and starts off.",Icons.Outlined.History)
     history.forEach { item -> Action("${item.score}/100 · ${item.level}",DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(item.timestamp))+"\n"+item.preview,Icons.Outlined.Shield) { vm.result.value=Gson().fromJson(item.resultJson,Analysis::class.java) } }
     if(history.isNotEmpty()) OutlinedButton(onClick={clearConfirm=true}) { Text("Delete All History") }
    }
    "Settings" -> {
     Text("Privacy & protection",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
     Panel {
      Toggle("Live SMS Protection",settings.live&&smsGranted) { requestLive(it) }
      HorizontalDivider()
      Toggle("Save history",settings.history) { vm.history(it) }
      Text("Disabling history stops future saves. Use Delete All History to remove earlier summaries.",style=MaterialTheme.typography.bodySmall)
      HorizontalDivider()
      Toggle("Hindi / Hinglish detection",settings.hindi) { value -> vm.update { it.copy(hindi=value) } }
      Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Outlined.Lock,null); Text("  On-device analysis · Always on",fontWeight=FontWeight.Bold) }
      Text("This app has no Internet permission. Help opens in your browser.",style=MaterialTheme.typography.bodySmall)
     }
     Text("Notification threshold",style=MaterialTheme.typography.titleMedium)
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf(50,60,70,80).forEach { threshold -> FilterChip(selected=settings.threshold==threshold,onClick={vm.update { it.copy(threshold=threshold) }},label={Text("$threshold")}) } }
     OutlinedButton(onClick={clearConfirm=true},modifier=Modifier.fillMaxWidth()) { Text("Delete All History") }
     Action("Demo Mode","Seven ready-to-test messages",Icons.Outlined.PlayCircle) { go("Demo Mode") }
     Action("About / Privacy","How scoring and protection work",Icons.Outlined.PrivacyTip) { go("About / Privacy") }
    }
    "Demo Mode" -> {
     Text("See the warning signs",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
     Text("Hackathon demo · Tap a sample to analyze it instantly. Scores are rule-based, not calibrated probabilities.")
     demoMessages.forEach { demo -> Panel { Text(demo.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold); Text(demo.text); Text("Expected: ${demo.expected}",color=MaterialTheme.colorScheme.primary); Button(onClick={vm.input.value=demo.text;vm.analyze("Demo")},enabled=!busy,modifier=Modifier.testTag("demo-${demo.title}")) { Text("Analyze sample") } } }
    }
    else -> {
     Text("Built for investor resilience",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
     Notice("Your messages are analyzed on your device. Sensitive credentials such as OTPs are not stored.",Icons.Outlined.PrivacyTip)
     Panel { Text("Privacy by design",style=MaterialTheme.typography.titleLarge); Text("• SMS protection starts OFF and requires explicit permission.\n• No existing inbox access, analytics, or message logging. SMS analysis is local.\n• OCR models are bundled for offline use.\n• History starts OFF and saves summaries only.\n• Backups are disabled.\n• Notification details omit original message evidence.\n• Help opens an external website in your browser. No message text is sent automatically.") }
     Panel { Text("Understand the score",style=MaterialTheme.typography.titleLarge); Text("0–24: Low red-flag score\n25–49: Some warning signs\n50–74: High risk\n75–100: Severe warning\n\nComponents: language 30%, URL 25%, impersonation 20%, financial patterns 15%, credentials 10%. Strong rules apply score floors, explained on the result screen.\n\nA score is not a probability or a verdict. False positives and missed scams are possible. The local organization list may be incomplete or become outdated.") }
     Text("SANGYAN Shield v1.0\nInvestor protection hackathon prototype. No stock tips, buy/sell/hold recommendations, price predictions, or trading advice.")
    }
   }
   Spacer(Modifier.height(12.dp))
  }
 }
}
@Composable private fun Panel(content:@Composable ColumnScope.()->Unit) { Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content) } }
@Composable private fun Notice(text:String,icon:ImageVector) { Surface(color=MaterialTheme.colorScheme.secondaryContainer.copy(alpha=.55f),shape=RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) { Icon(icon,null,Modifier.size(22.dp)); Text(text,style=MaterialTheme.typography.bodyMedium) } } }
@Composable private fun Toggle(title:String,value:Boolean,onChange:(Boolean)->Unit) { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Text(title,Modifier.weight(1f),fontWeight=FontWeight.Medium); Switch(value,onChange) } }
@Composable private fun Action(title:String,subtitle:String,icon:ImageVector,onClick:()->Unit) { OutlinedCard(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)) { Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) { Icon(icon,null,tint=MaterialTheme.colorScheme.primary); Column(Modifier.weight(1f)) { Text(title,fontWeight=FontWeight.SemiBold); Text(subtitle,style=MaterialTheme.typography.bodySmall) }; Icon(Icons.Outlined.ChevronRight,null) } } }
@Composable private fun SmallAction(title:String,icon:ImageVector,modifier:Modifier,onClick:()->Unit) { OutlinedCard(onClick=onClick,modifier=modifier,shape=RoundedCornerShape(20.dp)) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) { Icon(icon,null,tint=MaterialTheme.colorScheme.primary); Text(title,fontWeight=FontWeight.SemiBold) } } }
@Composable private fun ResultContent(a:Analysis) {
 val color=when { a.score>=75 -> Color(0xFFB3261E); a.score>=50 -> Color(0xFFAD4C10); a.score>=25 -> Color(0xFF846500); else -> Teal }
 Panel {
  Text("RED FLAG SCORE",style=MaterialTheme.typography.labelLarge,modifier=Modifier.align(Alignment.CenterHorizontally))
  Box(Modifier.size(210.dp).align(Alignment.CenterHorizontally).semantics { contentDescription="Red Flag Score ${a.score} out of 100, ${a.level}" },contentAlignment=Alignment.Center) {
   Canvas(Modifier.fillMaxSize().padding(14.dp)) { drawArc(color.copy(alpha=.15f),135f,270f,false,style=Stroke(18.dp.toPx(),cap=StrokeCap.Round)); if(a.score>0) drawArc(color,135f,270f*a.score/100f,false,style=Stroke(18.dp.toPx(),cap=StrokeCap.Round)) }
   Column(horizontalAlignment=Alignment.CenterHorizontally) { Text("${a.score}",fontSize=62.sp,fontWeight=FontWeight.Bold); Text("OUT OF 100",style=MaterialTheme.typography.labelMedium) }
  }
  Surface(color=color,shape=RoundedCornerShape(50),modifier=Modifier.align(Alignment.CenterHorizontally)) { Text(a.level,color=Color.White,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=20.dp,vertical=10.dp)) }
  Text(if(a.score<25) "No major warning signs were detected. This does not guarantee the message is legitimate." else "Pause before acting. These warning signs need independent verification.")
  Text(a.scoringNote,style=MaterialTheme.typography.bodySmall)
 }
 if(a.findings.isNotEmpty()) {
  Text("Why this was flagged",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
  a.findings.forEach { f -> Panel { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Icon(Icons.Outlined.WarningAmber,null,tint=color); Text(f.category,fontWeight=FontWeight.Bold) }; Text("“${org.sangyan.shield.privacy.SensitiveDataMasker.mask(f.evidence)}”",color=MaterialTheme.colorScheme.primary); Text(f.explanation,style=MaterialTheme.typography.bodyMedium) } }
 }
 Panel { Text("Risk components",style=MaterialTheme.typography.titleLarge); a.components.forEach { (name,value) -> Row { Text(name.replaceFirstChar { it.uppercase() },Modifier.weight(1f)); Text("$value / 100") }; LinearProgressIndicator(progress={value/100f},modifier=Modifier.fillMaxWidth(),color=color) } }
 if(a.entities.urls.isNotEmpty() || a.entities.organizations.isNotEmpty() || a.entities.emails.isNotEmpty() || a.entities.amounts.isNotEmpty() || a.entities.phones.isNotEmpty() || a.entities.upi.isNotEmpty() || a.entities.otpLike.isNotEmpty() || a.entities.registrations.isNotEmpty()) Panel {
  Text("Extracted details",style=MaterialTheme.typography.titleLarge)
  listOf("Links" to a.entities.urls,"Organizations" to a.entities.organizations,"Email addresses" to a.entities.emails,"Phone numbers" to a.entities.phones,"Amounts" to a.entities.amounts,"UPI IDs" to a.entities.upi,"Registration IDs (unverified)" to a.entities.registrations,"OTP-like values (masked)" to a.entities.otpLike).filter { it.second.isNotEmpty() }.forEach { (label,values) -> Text(label,fontWeight=FontWeight.Bold); Text(values.joinToString("\n") { org.sangyan.shield.privacy.SensitiveDataMasker.mask(it) }) }
  Text("Links are shown as text and are never opened or fetched.",style=MaterialTheme.typography.bodySmall)
 }
 Notice("Do not click suspicious links or pay requested fees. Never share OTP/PIN/CVV or passwords. Open the institution’s official app or website manually and contact it through verified channels.",Icons.Outlined.VerifiedUser)
}
