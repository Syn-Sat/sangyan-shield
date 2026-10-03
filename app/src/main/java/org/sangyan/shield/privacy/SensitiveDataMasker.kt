package org.sangyan.shield.privacy
import org.sangyan.shield.domain.model.*
object SensitiveDataMasker {
 fun mask(text:String):String = text
  .replace(Regex("(?i)((?:otp|pin|cvv|password|passcode)\\s*(?:is|:|=)?\\s*)[\\w@#$!%&*+.-]+")) { it.groupValues[1]+"[REDACTED]" }
  .replace(Regex("\\p{N}{3,}")) { "*".repeat(it.value.length) }
 /** Durable history contains no message body, sender, URL paths, query values or extracted credentials. */
 fun forStorage(a:Analysis):Analysis = a.copy(
  findings = a.findings.map { it.copy(evidence="[Evidence omitted from saved history]") },
  entities = Entities(organizations=a.entities.organizations),
  source = if(a.source=="SMS") "SMS" else if(a.source=="Screenshot") "Screenshot" else "Manual / demo")
}
