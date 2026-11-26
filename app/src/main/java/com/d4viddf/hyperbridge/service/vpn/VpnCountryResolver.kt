package com.d4viddf.hyperbridge.service.vpn

import java.text.Normalizer
import java.util.Locale

data class VpnCountry(val isoCode: String) {
    val flagEmoji: String get() = VpnCountryResolver.flagEmoji(isoCode)
}

/** Resolves only explicit flags, country codes, and localized country names. */
object VpnCountryResolver {
    private val isoCodes = Locale.getISOCountries().toSet()
    private val explicitCode = Regex("(?<![\\p{L}])([A-Z]{2})(?![\\p{L}])")
    private val relayCode = Regex("^[^\\p{L}]*([a-zA-Z]{2})[-_]")
    private val commonAliases = mapOf(
        "usa" to "US",
        "united states of america" to "US",
        "uk" to "GB",
        "uae" to "AE",
        "south korea" to "KR",
        "north korea" to "KP",
        "russia" to "RU",
        "vietnam" to "VN",
        "czech republic" to "CZ"
    )

    fun resolve(
        text: String,
        preferredLocales: List<Locale> = listOf(Locale.getDefault(), Locale.ENGLISH)
    ): VpnCountry? {
        extractFlagCode(text)?.let { return VpnCountry(it) }

        explicitCode.findAll(text).firstNotNullOfOrNull { match ->
            canonicalCode(match.groupValues[1])
        }?.let { return VpnCountry(it) }
        relayCode.find(text)?.groupValues?.get(1)?.let(::canonicalCode)?.let { return VpnCountry(it) }

        val normalizedText = " ${normalize(text)} "
        val aliases = buildAliases(preferredLocales)
        aliases.entries
            .sortedByDescending { it.key.length }
            .firstOrNull { (alias, _) -> " $alias " in normalizedText }
            ?.value
            ?.let { return VpnCountry(it) }
        return null
    }

    fun flagEmoji(isoCode: String): String {
        val code = canonicalCode(isoCode) ?: return ""
        return buildString {
            code.forEach { letter ->
                appendCodePoint(REGIONAL_INDICATOR_A + (letter - 'A'))
            }
        }
    }

    private fun extractFlagCode(text: String): String? {
        val codePoints = text.codePoints().toArray()
        for (index in 0 until codePoints.lastIndex) {
            val first = codePoints[index]
            val second = codePoints[index + 1]
            if (first in REGIONAL_INDICATOR_A..REGIONAL_INDICATOR_Z &&
                second in REGIONAL_INDICATOR_A..REGIONAL_INDICATOR_Z
            ) {
                return "${'A' + (first - REGIONAL_INDICATOR_A)}${'A' + (second - REGIONAL_INDICATOR_A)}"
                    .takeIf(isoCodes::contains)
            }
        }
        return null
    }

    private fun buildAliases(preferredLocales: List<Locale>): Map<String, String> = buildMap {
        putAll(commonAliases)
        val locales = (preferredLocales + SUPPORTED_PROVIDER_LOCALES).distinctBy(Locale::toLanguageTag)
        isoCodes.forEach { code ->
            val country = Locale.Builder().setRegion(code).build()
            locales.forEach { locale ->
                normalize(country.getDisplayCountry(locale))
                    .takeIf { it.length >= MIN_COUNTRY_NAME_LENGTH }
                    ?.let { putIfAbsent(it, code) }
            }
        }
    }

    private fun canonicalCode(value: String): String? = value.uppercase(Locale.ROOT).takeIf {
        it.length == 2 && it in isoCodes
    }

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .lowercase(Locale.ROOT)
        .replace(NON_ALPHANUMERIC, " ")
        .trim()
        .replace(MULTIPLE_SPACES, " ")

    private const val REGIONAL_INDICATOR_A = 0x1F1E6
    private const val REGIONAL_INDICATOR_Z = 0x1F1FF
    private const val MIN_COUNTRY_NAME_LENGTH = 4
    private val SUPPORTED_PROVIDER_LOCALES = listOf(Locale.ENGLISH, Locale.forLanguageTag("tr"))
    private val COMBINING_MARKS = Regex("\\p{M}+")
    private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")
    private val MULTIPLE_SPACES = Regex("\\s+")
}

