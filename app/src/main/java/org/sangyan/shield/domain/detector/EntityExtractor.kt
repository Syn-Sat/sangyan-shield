package org.sangyan.shield.domain.detector
import org.sangyan.shield.domain.model.Entities
class EntityExtractor(private val config: DetectorConfig) {
 fun extract(text: String): Entities {
  fun matches(p: String) = Regex(p, RegexOption.IGNORE_CASE).findAll(text).map { it.value }.distinct().toList()
  val urls = matches("""\b(?:https?://|www\.)[^\s<>]+|(?<![@\w])(?:[a-z0-9-]+\.)+(?:com|in|org|net|xyz|top|ly|co|click|vip|zip|work|io|me|gl|gy)(?:/[^\s<>]*)?""").map { it.trimEnd('.', ',', ')', ']', '!', ';', '"') }.distinct()
  val orgs = config.organizations.keys.filter { name -> Regex("(?i)\\b" + Regex.escape(name.removeSuffix(" Bank")) + "\\b").containsMatchIn(text) }
  return Entities(urls, matches("""[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}"""), matches("""(?<!\d)(?:\+91[ -]?)?[6-9]\d{9}(?!\d)"""), matches("""(?:₹|INR\s*|Rs\.?\s*)[\d,]+(?:\.\d{1,2})?"""), matches("""[\w.-]+@(?:upi|ybl|ibl|axl|ok\w+|paytm|sbi)\b"""), orgs, matches("""\bIN[A-Z]{1,3}\d{6,12}\b"""), matches("""(?<!\d)\d{4,8}(?!\d)""").map { "*".repeat(it.length) })
 }
}
