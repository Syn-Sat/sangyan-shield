package org.sangyan.shield
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.google.gson.Gson
import org.sangyan.shield.domain.model.Analysis
import org.sangyan.shield.ui.ShieldViewModel
import org.sangyan.shield.ui.ShieldUi
class MainActivity:ComponentActivity() {
 private val vm:ShieldViewModel by viewModels()
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge(); receive(intent); setContent { ShieldUi(vm) } }
 override fun onNewIntent(intent:Intent) { super.onNewIntent(intent); setIntent(intent); receive(intent) }
 private fun receive(intent:Intent?) {
  intent?.getStringExtra("analysis")?.let { json -> runCatching { Gson().fromJson(json,Analysis::class.java) }.getOrNull()?.let { vm.result.value=it } }
  intent?.removeExtra("analysis")
 }
}
