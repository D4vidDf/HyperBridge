package com.d4viddf.hyperbridge.util

import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

object DocumentationUrls {
    const val BASE_URL = "https://hyper-bridge.app"
    const val DOCS = "https://hyper-bridge.app/docs/"
    const val PRIVACY_POLICY = "https://hyper-bridge.app/privacy/"
    const val CUSTOMIZATION_DOCS = "https://hyper-bridge.app/docs/customization/"
    const val THEME_CREATOR_DOCS = "https://hyper-bridge.app/docs/customization/theme-creator/"
    const val GITHUB_BUG_REPORT = "https://github.com/D4vidDf/HyperBridge/issues/new?template=bug_report.yml"

    /**
     * Resolves the localized changelog URL on https://hyper-bridge.app/ based on the active app locale.
     * Supported website translations include:
     * es, es-419, cs, de, fr, hu, id, it, ja, ko, pl, pt-br, ru, sk, tr, uk, zh-tw.
     * Defaults to English at /changelog/.
     */
    fun getChangelogUrl(customLocale: Locale? = null): String {
        val locale = customLocale ?: getEffectiveLocale()
        val lang = locale.language.lowercase()
        val country = locale.country.lowercase()
        val script = locale.script.lowercase()

        val prefix = when {
            lang == "es" && (country == "419" || country == "em" || (country.isNotEmpty() && country != "es")) -> "es-419"
            lang == "es" -> "es"
            lang == "cs" -> "cs"
            lang == "de" -> "de"
            lang == "fr" -> "fr"
            lang == "hu" -> "hu"
            lang == "id" || lang == "in" -> "id"
            lang == "it" -> "it"
            lang == "ja" -> "ja"
            lang == "ko" -> "ko"
            lang == "pl" -> "pl"
            lang == "pt" -> "pt-br"
            lang == "ru" -> "ru"
            lang == "sk" -> "sk"
            lang == "tr" -> "tr"
            lang == "uk" -> "uk"
            lang == "zh" && (country == "tw" || country == "hk" || country == "mo" || script == "hant") -> "zh-tw"
            else -> null
        }

        return if (prefix != null) "$BASE_URL/$prefix/changelog/" else "$BASE_URL/changelog/"
    }

    private fun getEffectiveLocale(): Locale {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (!appLocales.isEmpty) {
            val primary = appLocales.get(0)
            if (primary != null) return primary
        }
        return Locale.getDefault()
    }
}
