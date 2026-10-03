package org.sangyan.shield.domain.detector
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.sangyan.shield.domain.model.*
class DetectorConfig(read: (String) -> String) {
 private val gson = Gson()
 val english: List<PhraseRule> = gson.fromJson(read("scam_phrases.json"), object: TypeToken<List<PhraseRule>>(){}.type)
 val hindi: List<PhraseRule> = gson.fromJson(read("hinglish_phrases.json"), object: TypeToken<List<PhraseRule>>(){}.type)
 val organizations: Map<String, List<String>> = gson.fromJson(read("organizations.json"), object: TypeToken<Map<String, List<String>>>(){}.type)
 val urls: UrlRules = gson.fromJson(read("url_rules.json"), UrlRules::class.java)
}
