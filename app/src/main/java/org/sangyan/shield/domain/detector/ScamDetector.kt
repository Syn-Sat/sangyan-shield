package org.sangyan.shield.domain.detector
import org.sangyan.shield.domain.model.*
import kotlin.math.roundToInt
class ScamDetector(private val config: DetectorConfig) {
 private val extractor = EntityExtractor(config)
 private val language = LanguageAnalyzer(config)
 private val urls = UrlAnalyzer(config)
 private val impersonation = ImpersonationAnalyzer(config, urls)
 fun analyze(input: String, hindi: Boolean = true, source: String = "Manual"): Analysis {
  val text = input.take(20000)
  val entities = extractor.extract(text)
  val hits = language.analyze(text, hindi).hits
  val urlResults = entities.urls.map(urls::analyze)
  val impersonations = impersonation.analyze(entities)
  val financialCategories = setOf("Guaranteed returns","Investment group","Unverified advisory","Advance fee","Remote access","Speculative investment pitch","KYC pressure","Account threat")
  val components = linkedMapOf(
   "language" to hits.sumOf { it.first.weight }.coerceAtMost(100),
   "url" to (urlResults.maxOfOrNull { it.first } ?: 0),
   "impersonation" to if(impersonations.isNotEmpty()) 95 else 0,
   "financial" to hits.filter { it.first.category in financialCategories }.sumOf { it.first.weight }.coerceAtMost(100),
   "credential" to if(hits.any { it.first.category == "Credential request" }) 100 else 0)
  val weighted = components.entries.sumOf { (key,value) -> value * (config.urls.weights[key] ?: 0) }.toDouble() / config.urls.weights.values.sum().coerceAtLeast(1)
  val floor = hits.maxOfOrNull { it.first.floor } ?: 0
  val combination = if(impersonations.isNotEmpty() && hits.isNotEmpty()) 85 else if(impersonations.isNotEmpty()) 60 else 0
  val score = maxOf(weighted.roundToInt(),floor,combination).coerceIn(0,100)
  val findings = hits.map { (rule, evidence) -> Finding(rule.category,evidence,when(rule.category) {
   "Credential request" -> "Requests for OTPs, PINs, CVVs or passwords can enable account theft."
   "Guaranteed returns" -> "Assured investment profits are a major warning sign."
   "Remote access" -> "Remote control software can expose financial accounts and credentials."
   "Advance fee" -> "Requests for a fee to release funds are a common scam pattern."
   else -> "This phrase matches a local warning pattern. Review it in context and verify through official channels."
  }) } + urlResults.flatMap { it.second } + impersonations
  return Analysis(score,components,findings.distinct(),entities,source=source,scoringNote=if(score>weighted.roundToInt()) "A high-impact rule raised the score above the weighted component total. This is a rule score, not a probability." else "Weighted rule score; not a probability of fraud.")
 }
}
interface ScamMlClassifier { suspend fun predict(text: String): Float }
/** Extension point only. No model inference or probability claims in version 1. */
class NoOpScamMlClassifier: ScamMlClassifier { override suspend fun predict(text: String) = 0f }
