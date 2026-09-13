package com.d4viddf.hyperbridge.service.smartactions

import com.d4viddf.hyperbridge.models.SmartAction
import com.d4viddf.hyperbridge.models.SmartActionType
import com.d4viddf.hyperbridge.models.SmartActionsConfig

/**
 * Pure-Kotlin entity extractor behind Smart Actions (issue #270).
 *
 * Deliberately free of Android imports so it can be unit tested on the JVM. Everything here is a
 * handful of regex passes over a few hundred characters, so it is cheap enough to run on the
 * notification worker thread right before translation.
 *
 * Priority when several entities are present: OTP > tracking number > URL > phone. Only the best
 * candidate of each type is returned, and the total is capped by [DEFAULT_MAX_ACTIONS] so the
 * island never fills up with buttons.
 */
object SmartActionsExtractor {

    const val DEFAULT_MAX_ACTIONS = 2

    fun extract(
        text: String?,
        config: SmartActionsConfig,
        maxActions: Int = DEFAULT_MAX_ACTIONS
    ): List<SmartAction> {
        if (!config.enabled || maxActions <= 0) return emptyList()
        val source = text?.trim().orEmpty()
        if (source.isEmpty()) return emptyList()

        val taken = mutableListOf<IntRange>()
        val results = mutableListOf<SmartAction>()

        if (config.otp) {
            findOtp(source)?.let { (action, range) ->
                results.add(action); taken.add(range)
            }
        }
        if (config.tracking) {
            findTracking(source, taken)?.let { (action, range) ->
                results.add(action); taken.add(range)
            }
        }
        if (config.url) {
            findUrl(source)?.let { (action, range) ->
                results.add(action); taken.add(range)
            }
        }
        if (config.phone) {
            findPhone(source, taken)?.let { (action, _) -> results.add(action) }
        }
        return results.take(maxActions)
    }

    // ------------------------------------------------------------------ OTP

    /**
     * Phrases that contain an OTP keyword but never introduce a code. They are blanked out before
     * keyword detection so "your zip code 28001" does not become a copy button.
     */
    private val OTP_NEGATIVE_PHRASES = Regex(
        "(?i)\\b(?:zip|postal|post|area|country|dial|promo|promotional|discount|coupon|referral|invite|order|qr|bar|error|status|http)\\s?codes?\\b" +
                "|\\bbarcodes?\\b" +
                "|\\bc[oó]digo\\s+(?:postal|promocional|de\\s+descuento|de\\s+barras|de\\s+pedido|de\\s+error|qr|de\\s+invitaci[oó]n)\\b" +
                "|\\bcodice\\s+(?:postale|sconto|promozionale)\\b" +
                "|\\bcode\\s+(?:postal|promo|de\\s+r[ée]duction)\\b" +
                "|\\bPostleitzahl\\b"
    )

    /** Latin-script OTP keywords (stems). Matched with Unicode-aware word boundaries. */
    private val OTP_KEYWORDS_LATIN = Regex(
        "(?i)(?<![\\p{L}\\p{N}])(?:" +
                "codes?|otp|passcodes?|pass\\s?codes?|passwords?|pins?|tokens?|" +
                "verif\\p{L}*|authenticat\\p{L}*|auth\\s?codes?|2fa|mfa|one[\\s-]?time|" +
                "c[oó]digos?|clave|contrase[ñn]a|seguridad|codice|senha|kods?|kodes?|" +
                "mot\\s+de\\s+passe|v[ée]rification|" +
                "best[äa]tigungscode|sicherheitscode|zugangscode|einmalpasswort|" +
                "do[ğg]rulama|[şs]ifre|wachtwoord|bekr[äa]ftelsekod|engangskode" +
                ")(?![\\p{L}\\p{N}])"
    )

    /** Non-Latin OTP keywords; substring match is correct for these scripts. */
    private val OTP_KEYWORDS_OTHER = Regex(
        "код|пароль|подтвержд|" +                 // ru / uk
                "验证码|驗證碼|校验码|动态码|" +             // zh
                "認証|確認コード|認証コード|ワンタイム|" +       // ja
                "인증|인증번호|" +                        // ko
                "mã xác|mã otp|" +                      // vi
                "رمز|كود|" +                            // ar
                "קוד"                                  // he
    )

    /**
     * 4-8 digit run, or two 3-digit groups joined by a space/dash ("123 456", "123-456").
     * Lookarounds reject digits that continue on either side (phone numbers, IBANs), decimals,
     * thousands separators, dates, and amounts next to a currency sign.
     */
    private val OTP_CANDIDATE = Regex(
        "(?i)(?<![\\p{L}\\p{N}+/€$£¥#])(?<!\\d[ .,-])" +
                "(\\d{3}[ -]\\d{3}|\\d{4,8})" +
                "(?![\\p{N}/-])(?![.,]\\d)(?![ -]\\d)(?!\\s?(?:[%€$£¥]|eur|usd|gbp|chf)(?![\\p{L}]))"
    )

    private val YEAR_LIKE = Regex("^(?:19|20)\\d{2}$")

    private const val OTP_MAX_DISTANCE_AFTER_KEYWORD = 120
    private const val OTP_MAX_DISTANCE_BEFORE_KEYWORD = 80
    private const val OTP_YEAR_MAX_DISTANCE = 12

    private fun findOtp(text: String): Pair<SmartAction, IntRange>? {
        val cleaned = OTP_NEGATIVE_PHRASES.replace(text) { m -> " ".repeat(m.value.length) }
        val keywords = (OTP_KEYWORDS_LATIN.findAll(cleaned) + OTP_KEYWORDS_OTHER.findAll(cleaned))
            .map { it.range }
            .toList()
        if (keywords.isEmpty()) return null

        var best: MatchResult? = null
        var bestScore = Int.MAX_VALUE
        for (candidate in OTP_CANDIDATE.findAll(cleaned)) {
            val digits = candidate.groupValues[1]
            val start = candidate.range.first
            val end = candidate.range.last + 1
            val yearLike = YEAR_LIKE.matches(digits)

            var score = Int.MAX_VALUE
            for (kw in keywords) {
                val kwStart = kw.first
                val kwEnd = kw.last + 1
                val s = when {
                    kwEnd <= start -> {
                        val d = start - kwEnd
                        if (d <= OTP_MAX_DISTANCE_AFTER_KEYWORD && (!yearLike || d <= OTP_YEAR_MAX_DISTANCE)) d else Int.MAX_VALUE
                    }
                    end <= kwStart -> {
                        val d = kwStart - end
                        // "123456 is your code" is common; "in 2026, verify..." is not: years must
                        // directly follow the keyword to count.
                        if (!yearLike && d <= OTP_MAX_DISTANCE_BEFORE_KEYWORD) d + 5 else Int.MAX_VALUE
                    }
                    else -> Int.MAX_VALUE
                }
                if (s < score) score = s
            }
            if (score < bestScore) {
                bestScore = score
                best = candidate
            }
        }
        val match = best ?: return null
        val code = match.groupValues[1].filter { it.isDigit() }
        return SmartAction(SmartActionType.OTP, value = code, target = code) to match.range
    }

    // ------------------------------------------------------------------ URL

    private const val URL_TLDS = "com|net|org|io|app|dev|es|eu|me|co|ly|to|gl|link|page|info|uk|de|fr|it|pt|nl|be|ch|at|br|mx|ar|cl|us|ca|au|in|jp|kr|cn|ru|tv|xyz|site|online|store|shop|cloud|ai|gg|id|ie|se|no|dk|fi|pl|cz|tr|gr|ro|hu"

    private val URL_CANDIDATE = Regex(
        "(?i)(?<![\\p{L}\\p{N}@/.])" +
                "((?:https?://|www\\.)[^\\s<>\"'`]+" +
                "|(?:[a-z0-9][a-z0-9-]*\\.)+(?:$URL_TLDS)(?:/[^\\s<>\"'`]*)?)" +
                "(?![\\p{L}\\p{N}])"
    )

    private val URL_TRAILING_PUNCT = Regex("[.,;:!?\\]}'\"»]+$")

    private fun findUrl(text: String): Pair<SmartAction, IntRange>? {
        for (m in URL_CANDIDATE.findAll(text)) {
            var raw = m.value
            // Strip sentence punctuation; keep a closing paren only if the URL itself opened one
            // (wikipedia-style links), otherwise it belongs to the surrounding prose.
            while (true) {
                var next = URL_TRAILING_PUNCT.replace(raw, "")
                if (next.endsWith(")") && next.count { it == '(' } < next.count { it == ')' }) next = next.dropLast(1)
                if (next == raw) break
                raw = next
            }
            val hasScheme = raw.startsWith("http://", true) || raw.startsWith("https://", true)
            if (!hasScheme) {
                val host = raw.substringBefore('/')
                val labels = host.removePrefix("www.").split('.')
                // Bare domains need a real label ("a.es" is almost always prose, "amzn.to" is a link).
                if (labels.size < 2 || labels.first().length < 2) continue
            }
            if (raw.length < 5) continue
            val target = if (hasScheme) raw else "https://$raw"
            return SmartAction(SmartActionType.URL, value = raw, target = target) to m.range
        }
        return null
    }

    // ------------------------------------------------------------------ PHONE

    private val PHONE_CANDIDATE = Regex(
        "(?<![\\p{L}\\p{N}/.\\-+])(\\+?\\(?\\d[\\d\\s().\\-]{5,20}\\d)(?![\\p{L}\\p{N}/\\-])(?!\\.\\d)(?!\\s?[%€$£¥])"
    )

    private val DATE_LIKE = Regex("^\\d{1,4}[-/.]\\d{1,2}[-/.]\\d{1,4}$")

    /** Words that make a preceding number an identifier rather than something to dial. */
    private val PHONE_NEGATIVE_CONTEXT = Regex(
        "(?i)(?<![\\p{L}])(?:order|orden|pedido|ref\\p{L}*|invoice|factura|account|cuenta|iban|tracking|seguimiento|env[ií]o|shipment|" +
                "amount|importe|total|balance|saldo|card|tarjeta|n[ºo°]\\.?|#|id|code|c[oó]digo|otp|pin)\\s*:?\\s*$"
    )

    private fun findPhone(text: String, taken: List<IntRange>): Pair<SmartAction, IntRange>? {
        for (m in PHONE_CANDIDATE.findAll(text)) {
            if (taken.any { it.overlaps(m.range) }) continue
            val raw = m.groupValues[1]
            if (DATE_LIKE.matches(raw.trim())) continue
            val digits = raw.filter { it.isDigit() }
            val international = raw.startsWith("+")
            val plausible = if (international) digits.length in 8..15 else digits.length in 9..15
            if (!plausible) continue
            // A bare 12+ digit run with no formatting is an id, not a number someone would dial.
            if (!international && raw.none { it == ' ' || it == '-' || it == '(' || it == '.' } && digits.length > 11) continue
            val before = text.substring(maxOf(0, m.range.first - 24), m.range.first)
            if (PHONE_NEGATIVE_CONTEXT.containsMatchIn(before)) continue
            val dialable = (if (international) "+" else "") + digits
            return SmartAction(SmartActionType.PHONE, value = raw.trim(), target = dialable) to m.range
        }
        return null
    }

    // ------------------------------------------------------------------ TRACKING

    private class CarrierPattern(
        val name: String,
        val regex: Regex,
        val requiresKeyword: Regex? = null,
        val url: (String) -> String
    )

    private val TRACKING_CONTEXT = Regex(
        "(?i)track|seguimiento|env[ií]o|enviado|shipment|shipped|shipping|parcel|paquete|package|colis|sendung|paket|" +
                "delivery|deliver|entrega|repartidor|courier|mensajer|expedici[oó]n|encomenda|rastre|spedizione|pacco|" +
                "lieferung|livraison|доставк|посылк|配送|荷物|배송|택배"
    )

    private const val GENERIC_TRACKER = "https://www.17track.net/en/track?nums="

    private val CARRIERS = listOf(
        CarrierPattern("UPS", Regex("\\b1Z[A-Z0-9]{16}\\b")) { "https://www.ups.com/track?tracknum=$it" },
        CarrierPattern("USPS", Regex("\\b9[2-5]\\d{18,20}\\b")) { "https://tools.usps.com/go/TrackConfirmAction?tLabels=$it" },
        CarrierPattern("DHL Express", Regex("\\bJJ?D\\d{18}\\b")) { "https://www.dhl.com/global-en/home/tracking.html?tracking-id=$it" },
        CarrierPattern("Amazon", Regex("\\bTBA\\d{12}\\b")) { GENERIC_TRACKER + it },
        CarrierPattern("Correos", Regex("\\b[A-Z]{2}\\d{9}ES\\b")) { "https://www.correos.es/es/es/herramientas/localizador/envios/detalle?tracking-number=$it" },
        CarrierPattern("Postal", Regex("\\b[A-Z]{2}\\d{9}[A-Z]{2}\\b")) { GENERIC_TRACKER + it },
        CarrierPattern("FedEx", Regex("\\b(?:\\d{12}|\\d{15}|\\d{20})\\b"), Regex("(?i)fedex")) { "https://www.fedex.com/fedextrack/?trknbr=$it" },
        CarrierPattern("DHL", Regex("\\b\\d{10}\\b"), Regex("(?i)\\bdhl\\b")) { "https://www.dhl.com/global-en/home/tracking.html?tracking-id=$it" },
        // Anything that looks like a shipment reference, only when the text is clearly about a parcel.
        CarrierPattern("Parcel", Regex("\\b(?=[A-Z0-9]*\\d)(?=[A-Z0-9]*[A-Z])[A-Z0-9]{10,30}\\b"), TRACKING_CONTEXT) { GENERIC_TRACKER + it },
        CarrierPattern("Parcel", Regex("(?<![\\d+-])(?<!\\d[.,])\\d{12,22}(?![\\d-])(?![.,]\\d)"), TRACKING_CONTEXT) { GENERIC_TRACKER + it }
    )

    private fun findTracking(text: String, taken: List<IntRange>): Pair<SmartAction, IntRange>? {
        for (carrier in CARRIERS) {
            if (carrier.requiresKeyword != null && !carrier.requiresKeyword.containsMatchIn(text)) continue
            for (m in carrier.regex.findAll(text)) {
                if (taken.any { it.overlaps(m.range) }) continue
                val id = m.value
                if (id.count { it.isDigit() } < 6) continue
                return SmartAction(
                    SmartActionType.TRACKING,
                    value = id,
                    target = carrier.url(id),
                    carrier = carrier.name
                ) to m.range
            }
        }
        return null
    }

    private fun IntRange.overlaps(other: IntRange): Boolean = first <= other.last && other.first <= last
}
