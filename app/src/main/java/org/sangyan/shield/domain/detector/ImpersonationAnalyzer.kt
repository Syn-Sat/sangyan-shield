package org.sangyan.shield.domain.detector
import org.sangyan.shield.domain.model.*
class ImpersonationAnalyzer(private val config: DetectorConfig, private val urls: UrlAnalyzer) {
 private fun distance(a:String,b:String):Int {
  var row = IntArray(b.length+1) { it }
  for(i in a.indices) { val next = IntArray(b.length+1); next[0]=i+1; for(j in b.indices) next[j+1]=minOf(next[j]+1,row[j+1]+1,row[j]+if(a[i]==b[j])0 else 1); row=next }; return row[b.length]
 }
 fun analyze(entities: Entities): List<Finding> = entities.urls.flatMap { link ->
  val host = urls.host(link)
  config.organizations.filter { (name, official) ->
   val token = name.lowercase().replace(" bank", "").replace(" ", "")
   val label = host.split('.').dropLast(1).lastOrNull().orEmpty().replace("-", "")
   val claims = name in entities.organizations || host.replace("-", "").contains(token) || (token.length >= 4 && distance(label,token) == 1)
   claims && official.none { host == it || host.endsWith(".$it") }
  }.map { (name, _) -> Finding("Possible $name impersonation", host, "The domain does not match the known official domain in the app's verification list. The list may be incomplete; verify independently.") }
 }
}
