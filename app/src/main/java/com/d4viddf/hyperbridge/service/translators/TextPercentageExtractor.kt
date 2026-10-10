package com.d4viddf.hyperbridge.service.translators

/**
 * Reads a progress percentage ("Downloading… 42%") out of a notification's title/text.
 *
 * A percentage that reads as a discount is not progress: "50% off", "-20%", "Save 30%" or
 * "20% de descuento" from a store app would otherwise become a progress bar, since store
 * packages are treated as downloads. A string with a discount word (whole word) is skipped.
 */
object TextPercentageExtractor {

    private val PERCENT = Regex("""\b(\d{1,3})\s*%""")

    /** "50% off" / "50 % OFF" right after the number. */
    private val OFF_AFTER = Regex("""^\s*off\b""", RegexOption.IGNORE_CASE)

    /** "-20%", "−20 %" (a signed percentage is a change, not progress). */
    private val SIGN_BEFORE = Regex("""[-−–+]\s*$""")

    private val DISCOUNT_WORDS = Regex(
        "(?<![\\p{L}\\p{N}])(?:" +
            // English
            "discounts?|discounted|sale|save|coupons?|promo|deals?|cashback|" +
            // Spanish / Galician / Portuguese
            "descuentos?|desconto|descontos|dto|rebajas?|rebaixas?|ofertas?|" +
            // Italian / French / German
            "sconto|sconti|remise|soldes|réduction|rabatt|" +
            // Russian / Ukrainian
            "скидк[аиуе]|знижк[аиу]" +
            ")(?![\\p{L}\\p{N}])",
        RegexOption.IGNORE_CASE
    )

    fun extract(title: String?, text: String?): Int? =
        text?.let(::fromString) ?: title?.let(::fromString)

    private fun fromString(value: String): Int? {
        if (DISCOUNT_WORDS.containsMatchIn(value)) return null
        for (match in PERCENT.findAll(value)) {
            val before = value.substring(0, match.range.first)
            val after = value.substring(match.range.last + 1)
            if (SIGN_BEFORE.containsMatchIn(before) || OFF_AFTER.containsMatchIn(after)) continue
            val percent = match.groupValues[1].toIntOrNull() ?: continue
            if (percent in 0..100) return percent
        }
        return null
    }
}
