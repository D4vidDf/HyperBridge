package com.d4viddf.hyperbridge.service.smartactions

import com.d4viddf.hyperbridge.models.SmartActionType
import com.d4viddf.hyperbridge.models.SmartActionsConfig

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartActionsExtractorTest {

    private val all = SmartActionsConfig(enabled = true)

    private fun extract(text: String, config: SmartActionsConfig = all, max: Int = 4) =
        SmartActionsExtractor.extract(text, config, max)

    private fun only(text: String, type: SmartActionType) =
        extract(text).singleOrNull { it.type == type }

    // ---------------------------------------------------------------- config

    @Test
    fun disabledConfigReturnsNothingEvenWithObviousCode() {
        assertTrue(extract("Your verification code is 483920", SmartActionsConfig.DISABLED).isEmpty())
    }

    @Test
    fun blankTextReturnsNothing() {
        assertTrue(extract("   ").isEmpty())
        assertTrue(SmartActionsExtractor.extract(null, all).isEmpty())
    }

    @Test
    fun disabledTypeIsSkipped() {
        val noOtp = all.copy(otp = false)
        assertNull(extract("Your verification code is 483920", noOtp).firstOrNull { it.type == SmartActionType.OTP })
    }

    @Test
    fun excludedPackageIsInactive() {
        val config = all.copy(excludedPackages = setOf("com.bank"))
        assertFalse(config.isActiveFor("com.bank"))
        assertTrue(config.isActiveFor("com.whatsapp"))
        assertFalse(SmartActionsConfig.DISABLED.isActiveFor("com.whatsapp"))
    }

    @Test
    fun maxActionsCapsTheResult() {
        val text = "Your code is 123456. Details: https://example.com/x"
        assertEquals(1, extract(text, max = 1).size)
        assertEquals(SmartActionType.OTP, extract(text, max = 1).first().type)
    }

    @Test
    fun priorityIsOtpThenTrackingThenUrlThenPhone() {
        val text = "Code 4321. Track 1Z999AA10123456784 at https://ups.com or call +34 612 345 678"
        val types = extract(text).map { it.type }
        assertEquals(
            listOf(SmartActionType.OTP, SmartActionType.TRACKING, SmartActionType.URL, SmartActionType.PHONE),
            types
        )
    }

    // ---------------------------------------------------------------- OTP

    @Test
    fun otpAfterKeyword() {
        assertEquals("483920", only("Your verification code is 483920", SmartActionType.OTP)?.value)
    }

    @Test
    fun otpBeforeKeywordWithGooglePrefix() {
        assertEquals("483920", only("G-483920 is your Google verification code.", SmartActionType.OTP)?.value)
    }

    @Test
    fun otpSplitWithSpaceIsJoined() {
        assertEquals("123456", only("Tu código de verificación es 123 456", SmartActionType.OTP)?.value)
    }

    @Test
    fun otpSplitWithDashIsJoined() {
        assertEquals("482193", only("Your code: 482-193", SmartActionType.OTP)?.value)
    }

    @Test
    fun otpWhatsAppStyle() {
        assertEquals("552901", only("<#> 552901 es tu código de WhatsApp", SmartActionType.OTP)?.value)
    }

    @Test
    fun otpFollowedBySentencePeriod() {
        assertEquals("7781", only("Ihr Bestätigungscode lautet 7781.", SmartActionType.OTP)?.value)
    }

    @Test
    fun otpPinKeyword() {
        assertEquals("4321", only("Your PIN is 4321", SmartActionType.OTP)?.value)
    }

    @Test
    fun otpTwoFactorKeyword() {
        assertEquals("90210", only("Use 2FA code 90210 to log in", SmartActionType.OTP)?.value)
    }

    @Test
    fun otpRussian() {
        assertEquals("774411", only("Ваш код: 774411", SmartActionType.OTP)?.value)
    }

    @Test
    fun otpChineseWithFullWidthPunctuation() {
        assertEquals("553311", only("【淘宝】验证码 553311，请勿泄露", SmartActionType.OTP)?.value)
    }

    @Test
    fun otpJapanese() {
        assertEquals("882211", only("認証コードは 882211 です", SmartActionType.OTP)?.value)
    }

    @Test
    fun otpNearestCandidateWins() {
        val text = "Order 55512345 shipped. Your verification code is 9021"
        assertEquals("9021", only(text, SmartActionType.OTP)?.value)
    }

    @Test
    fun otpIgnoresZipCode() {
        assertNull(only("Your zip code is 28001", SmartActionType.OTP))
    }

    @Test
    fun otpIgnoresSpanishPostalCode() {
        assertNull(only("Entrega en código postal 36680", SmartActionType.OTP))
    }

    @Test
    fun otpIgnoresTextWithoutKeyword() {
        assertNull(only("Meeting at 10:30 in room 2026", SmartActionType.OTP))
    }

    @Test
    fun otpIgnoresYearFarFromKeyword() {
        assertNull(only("Verification for the account created in 2024 failed", SmartActionType.OTP))
    }

    @Test
    fun otpIgnoresAmounts() {
        assertNull(only("Payment of 1500 EUR confirmed. Verify at the branch.", SmartActionType.OTP))
        assertNull(only("Verify: you paid 1234€ today", SmartActionType.OTP))
        assertNull(only("Verify: balance $1234 today", SmartActionType.OTP))
    }

    @Test
    fun otpIgnoresDigitsInsidePhoneNumber() {
        val text = "Your code is 123456. Call +34 612 345 678 for help"
        val actions = extract(text)
        assertEquals("123456", actions.first { it.type == SmartActionType.OTP }.value)
        assertEquals("+34612345678", actions.first { it.type == SmartActionType.PHONE }.target)
    }

    @Test
    fun otpIgnoresDecimalsAndThousands() {
        assertNull(only("Verification: total 1,234.56 charged", SmartActionType.OTP))
    }

    // ---------------------------------------------------------------- URL

    @Test
    fun urlWithSchemeStripsTrailingPeriod() {
        val action = only("Check https://example.com/path?x=1.", SmartActionType.URL)
        assertEquals("https://example.com/path?x=1", action?.target)
    }

    @Test
    fun urlWithWwwGetsHttps() {
        assertEquals("https://www.hyper-bridge.app", only("Visit www.hyper-bridge.app for docs", SmartActionType.URL)?.target)
    }

    @Test
    fun urlBareShortenerDomain() {
        assertEquals("https://amzn.to/3xYz", only("Track at amzn.to/3xYz", SmartActionType.URL)?.target)
    }

    @Test
    fun urlKeepsBalancedParentheses() {
        val action = only("See https://en.wikipedia.org/wiki/Island_(disambiguation) now", SmartActionType.URL)
        assertEquals("https://en.wikipedia.org/wiki/Island_(disambiguation)", action?.target)
    }

    @Test
    fun urlDropsUnbalancedClosingParen() {
        val action = only("(see https://example.com/a)", SmartActionType.URL)
        assertEquals("https://example.com/a", action?.target)
    }

    @Test
    fun urlIgnoresEmailAddresses() {
        assertNull(only("Contact us at support@example.com", SmartActionType.URL))
    }

    @Test
    fun urlIgnoresAbbreviations() {
        assertNull(only("e.g. this is fine, i.e. no link", SmartActionType.URL))
    }

    // ---------------------------------------------------------------- PHONE

    @Test
    fun phoneInternationalWithFormatting() {
        val action = only("Call us at +1 (415) 555-0134", SmartActionType.PHONE)
        assertEquals("+14155550134", action?.target)
        assertEquals("+1 (415) 555-0134", action?.value)
    }

    @Test
    fun phoneNationalNineDigits() {
        assertEquals("912345678", only("Llámanos al 912 345 678 para confirmar", SmartActionType.PHONE)?.target)
    }

    @Test
    fun phoneFollowedBySentencePeriod() {
        assertEquals("+34612345678", only("Llama al +34 612 345 678.", SmartActionType.PHONE)?.target)
    }

    @Test
    fun phoneIgnoresDates() {
        assertNull(only("Cita el 13/09/2026 a las 10:30", SmartActionType.PHONE))
        assertNull(only("Cita el 13-09-2026 a las 10:30", SmartActionType.PHONE))
    }

    @Test
    fun phoneIgnoresOrderNumbers() {
        assertNull(only("Pedido 2026091312345 confirmado", SmartActionType.PHONE))
        assertNull(only("Order 912 345 678 confirmed", SmartActionType.PHONE))
    }

    @Test
    fun phoneIgnoresIban() {
        assertNull(only("IBAN ES91 2100 0418 4502 0005 1332", SmartActionType.PHONE))
    }

    // ---------------------------------------------------------------- TRACKING

    @Test
    fun trackingUps() {
        val action = only("Your UPS package 1Z999AA10123456784 is out for delivery", SmartActionType.TRACKING)
        assertEquals("1Z999AA10123456784", action?.value)
        assertEquals("UPS", action?.carrier)
        assertTrue(action!!.target.startsWith("https://www.ups.com/track?tracknum=1Z999AA10123456784"))
    }

    @Test
    fun trackingCorreosUpuCode() {
        val action = only("Correos: tu envío RR123456789ES ya está en reparto", SmartActionType.TRACKING)
        assertEquals("Correos", action?.carrier)
        assertTrue(action!!.target.contains("correos.es"))
    }

    @Test
    fun trackingUsps() {
        val action = only("USPS 9400111899223033005712 delivered", SmartActionType.TRACKING)
        assertEquals("USPS", action?.carrier)
    }

    @Test
    fun trackingFedExNeedsKeyword() {
        val action = only("FedEx shipment 123456789012 will arrive tomorrow", SmartActionType.TRACKING)
        assertEquals("FedEx", action?.carrier)
        assertNull(only("Reference 123456789012 will arrive tomorrow", SmartActionType.TRACKING))
    }

    @Test
    fun trackingDhlNeedsKeyword() {
        assertEquals("DHL", only("DHL: your shipment 1234567890 is in transit", SmartActionType.TRACKING)?.carrier)
        assertNull(only("Your balance is 1234567890", SmartActionType.TRACKING))
    }

    @Test
    fun trackingAmazon() {
        assertEquals("Amazon", only("Package TBA123456789012 delivered", SmartActionType.TRACKING)?.carrier)
    }

    @Test
    fun trackingGenericAlphanumericWhenTextIsAboutAParcel() {
        val action = only("Tu paquete GLS con referencia 4F2Z9K8L1Q0 llega hoy", SmartActionType.TRACKING)
        assertEquals("4F2Z9K8L1Q0", action?.value)
        assertTrue(action!!.target.startsWith("https://www.17track.net/"))
    }

    @Test
    fun trackingGenericIgnoredWithoutParcelContext() {
        assertNull(only("Reference 4F2Z9K8L1Q0 for your records", SmartActionType.TRACKING))
    }

    @Test
    fun trackingDoesNotStealTheOtp() {
        val actions = extract("Your package code is 123456, use it at the locker")
        assertEquals("123456", actions.first { it.type == SmartActionType.OTP }.value)
        assertNull(actions.firstOrNull { it.type == SmartActionType.TRACKING })
    }

    // ---------------------------------------------------------------- NAVIGATION

    @Test
    fun navigationGoogleMapsLinkBecomesDirectionsNotOpenLink() {
        val actions = extract("Here's the restaurant: https://maps.google.com/?q=Casa+Marcelo+Santiago")
        val nav = actions.single { it.type == SmartActionType.NAVIGATION }
        assertEquals("https://maps.google.com/?q=Casa+Marcelo+Santiago", nav.target)
        assertNull(actions.firstOrNull { it.type == SmartActionType.URL })
    }

    @Test
    fun navigationShortGoogleMapsLink() {
        val nav = only("Meet here https://maps.app.goo.gl/AbC123xyz.", SmartActionType.NAVIGATION)
        assertEquals("https://maps.app.goo.gl/AbC123xyz", nav?.value)
    }

    @Test
    fun navigationGoogleMapsWithoutScheme() {
        val nav = only("Location: maps.google.es/maps?q=42.68,-8.48", SmartActionType.NAVIGATION)
        assertEquals("https://maps.google.es/maps?q=42.68,-8.48", nav?.target)
    }

    @Test
    fun navigationAppleMapsAndWaze() {
        assertEquals("https://maps.apple.com/?ll=42.88,-8.54", only("Pin: https://maps.apple.com/?ll=42.88,-8.54", SmartActionType.NAVIGATION)?.target)
        assertEquals("https://waze.com/ul/hsv9x2k3", only("Drive with me https://waze.com/ul/hsv9x2k3", SmartActionType.NAVIGATION)?.target)
    }

    @Test
    fun navigationGeoUri() {
        val nav = only("Your driver shared geo:42.6814,-8.4820?q=Pickup", SmartActionType.NAVIGATION)
        assertEquals("geo:42.6814,-8.4820?q=Pickup", nav?.target)
    }

    @Test
    fun navigationOrdinaryLinkStillOpensAsLink() {
        val actions = extract("Read more at https://example.com/news/123")
        assertEquals(SmartActionType.URL, actions.single().type)
    }

    @Test
    fun navigationDisabledTypeIsSkipped() {
        val noNav = all.copy(navigation = false)
        // With navigation off a map link falls through to the ordinary link button.
        assertEquals(SmartActionType.URL, extract("https://maps.app.goo.gl/AbC123xyz", noNav).single().type)
    }

    @Test
    fun navigationTrailingPunctuationIsNotPartOfTheLink() {
        assertEquals("https://maps.apple.com/?q=Coffee", only("See you there (https://maps.apple.com/?q=Coffee).", SmartActionType.NAVIGATION)?.target)
    }

    @Test
    fun navigationOtpStillWinsAndCountsTowardsTheCap() {
        val actions = extract("Your code is 483920. Pickup: https://maps.app.goo.gl/AbC123xyz. Track at https://example.com/t", max = 2)
        assertEquals(listOf(SmartActionType.OTP, SmartActionType.NAVIGATION), actions.map { it.type })
    }

    @Test
    fun navigationOnlyOnePerNotification() {
        val actions = extract("From https://maps.app.goo.gl/AbC123xyz to https://maps.app.goo.gl/Zyx987cba", max = 4)
        assertEquals(1, actions.count { it.type == SmartActionType.NAVIGATION })
        assertEquals("https://maps.app.goo.gl/AbC123xyz", actions.single { it.type == SmartActionType.NAVIGATION }.value)
    }
}
