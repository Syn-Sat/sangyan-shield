package org.sangyan.shield.domain.detector
import org.sangyan.shield.domain.model.*
data class LanguageResult(val hits: List<Pair<PhraseRule, String>>)
class LanguageAnalyzer(private val config: DetectorConfig) {
 fun analyze(text: String, hindi: Boolean): LanguageResult {
  // Negation applies only to its sentence/clause, never to the entire message.
  val clauses = text.split(Regex("[.!?;\\n]|\\bbut\\b", RegexOption.IGNORE_CASE))
  val protective = Regex("""(?:never|do not|don't|avoid|will not|cannot)\s+(?:(?:ever|ask|you|anyone|to|for|be|asked|offer|promise|provide)\s+){0,6}$|(?:beware(?: of)?|warning against)\s*$|(?:मत|कभी नहीं|na kare|mat kare)\s*$""", RegexOption.IGNORE_CASE)
  val hits = (config.english + if(hindi) config.hindi else emptyList()).mapNotNull { rule ->
   val regex = Regex(rule.pattern, RegexOption.IGNORE_CASE)
   val hit = clauses.asSequence().flatMap { clause ->
    regex.findAll(clause).filter { match ->
     val before = clause.substring(0, match.range.first)
     // A warning after a request must not suppress that request.
     !protective.containsMatchIn(before) && !protective.containsMatchIn(match.value)
    }
   }.firstOrNull()
   hit?.let { rule to it.value }
  }
  return LanguageResult(hits)
 }
}
