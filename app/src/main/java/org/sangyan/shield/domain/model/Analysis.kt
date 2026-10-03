package org.sangyan.shield.domain.model

data class Finding(val category: String, val evidence: String, val explanation: String)
data class Entities(val urls: List<String> = emptyList(), val emails: List<String> = emptyList(), val phones: List<String> = emptyList(), val amounts: List<String> = emptyList(), val upi: List<String> = emptyList(), val organizations: List<String> = emptyList(), val registrations: List<String> = emptyList(), val otpLike: List<String> = emptyList())
data class Analysis(val score: Int, val components: Map<String, Int>, val findings: List<Finding>, val entities: Entities, val timestamp: Long = System.currentTimeMillis(), val source: String = "Manual", val scoringNote: String = "") {
 val level: String get() = when { score >= 75 -> "SEVERE WARNING"; score >= 50 -> "HIGH RISK"; score >= 25 -> "SOME WARNING SIGNS"; else -> "LOW RED-FLAG SCORE" }
}
data class PhraseRule(val category: String, val pattern: String, val weight: Int, val floor: Int)
data class UrlRules(val shorteners: List<String>, val suspiciousTlds: List<String>, val weights: Map<String, Int>)
