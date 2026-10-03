package org.sangyan.shield.domain.detector
import java.net.URI
import java.net.IDN
import org.sangyan.shield.domain.model.Finding
class UrlAnalyzer(private val config: DetectorConfig) {
 fun host(url: String): String = runCatching {
  val uri = URI(if(url.contains("://")) url else "https://$url")
  val authority = uri.rawAuthority.orEmpty().substringAfterLast('@').substringBefore(':')
  IDN.toASCII(authority).lowercase().trimEnd('.')
 }.getOrDefault("")
 fun analyze(url: String): Pair<Int, List<Finding>> {
  val host = host(url)
  val findings = mutableListOf<Finding>()
  var score = 0
  fun add(points: Int, reason: String) { score += points; findings += Finding("Suspicious website", host.ifEmpty { "Unparseable URL" }, reason) }
  if(host.isBlank()) add(55,"The website address could not be parsed reliably.")
  if(url.startsWith("http://",true)) add(20,"HTTP does not encrypt the connection.")
  if(config.urls.shorteners.any { host == it }) add(25,"Shortened link hides its destination; this alone does not establish fraud.")
  if(host.substringAfterLast('.') in config.urls.suspiciousTlds) add(30,"This domain ending is a heuristic warning sign, not proof of fraud.")
  if(Regex("^\\d{1,3}(\\.\\d{1,3}){3}$").matches(host)) add(45,"The link uses an IP address instead of a named institution.")
  if(host.count { it == '-' } >= 2) add(20,"Multiple hyphens can be used in imitation domains.")
  if(host.contains("xn--")) add(30,"Internationalized/punycode domain: check for lookalike characters.")
  if(url.substringBefore('?').contains('@')) add(45,"An @ symbol can conceal the real website host.")
  if(host.count { it == '.' } >= 4) add(25,"Many subdomains can conceal the actual destination.")
  if(host.length > 45) add(20,"Unusually long domain name.")
  if(url.contains('%')) add(15,"Encoded URL characters may obscure the destination.")
  if(Regex("[?&](otp|pin|password|cvv|redirect|url|token)=",RegexOption.IGNORE_CASE).containsMatchIn(url)) add(30,"Query parameters may transmit credentials or redirect you.")
  return score.coerceIn(0,100) to findings
 }
}
