package com.d4viddf.hyperbridge.service.translators

import com.d4viddf.hyperbridge.models.translator.CompactPillConfig
import com.d4viddf.hyperbridge.models.translator.IslandTemplateCatalog
import com.d4viddf.hyperbridge.models.translator.PillIconSource
import com.d4viddf.hyperbridge.models.translator.PillLeftDesign
import com.d4viddf.hyperbridge.models.translator.PillRightDesign
import com.d4viddf.hyperbridge.models.translator.PillTextSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DynamicTranslatorExtractionTest {

    @Test
    fun testBoardingPassGateExtraction() {
        assertEquals("Gate B12", DynamicTranslator.extractBoardingPassFromText("Flight to JFK. Proceed to Gate B12 now."))
        assertEquals("Gate 4", DynamicTranslator.extractBoardingPassFromText("Vuelo a Madrid. Puerta 4 embarcando."))
        assertEquals("Gate A1", DynamicTranslator.extractBoardingPassFromText("Vol vers Paris. Porte A1."))
        assertEquals("Gate C22", DynamicTranslator.extractBoardingPassFromText("Flug nach Frankfurt. Tor C22."))
    }

    @Test
    fun testBoardingPassTimeExtraction() {
        assertEquals("Boarding 14:30", DynamicTranslator.extractBoardingPassFromText("Your flight is on time. Boarding at 14:30."))
        assertEquals("Boarding 08:15", DynamicTranslator.extractBoardingPassFromText("Embarque 08:15 en puerta."))
    }

    @Test
    fun testBoardingPassFlightNumberExtraction() {
        assertEquals("IB 3166", DynamicTranslator.extractBoardingPassFromText("Flight IB 3166 to London Heathrow."))
        assertEquals("AF 1234", DynamicTranslator.extractBoardingPassFromText("AF 1234 CDG -> JFK."))
        assertEquals("LH 400", DynamicTranslator.extractBoardingPassFromText("Lufthansa LH 400 scheduled."))
    }

    @Test
    fun testBoardingPassSeatExtraction() {
        assertEquals("Seat 14B", DynamicTranslator.extractBoardingPassFromText("Your assigned seat is Seat 14B."))
        assertEquals("Seat 3F", DynamicTranslator.extractBoardingPassFromText("Asiento 3F preferente."))
        assertEquals("Seat 22A", DynamicTranslator.extractBoardingPassFromText("Platz 22A am Fenster."))
    }

    @Test
    fun testBoardingPassBlankReturnsNull() {
        assertNull(DynamicTranslator.extractBoardingPassFromText(""))
        assertNull(DynamicTranslator.extractBoardingPassFromText("   "))
        assertNull(DynamicTranslator.extractBoardingPassFromText("Regular message without flight details."))
    }

    @Test
    fun testOtpExtraction() {
        assertEquals("123456", DynamicTranslator.extractOtpFromText("Your verification code is 123456."))
        assertEquals("9876", DynamicTranslator.extractOtpFromText("Su código de seguridad es 9876"))
        assertEquals("543210", DynamicTranslator.extractOtpFromText("Use OTP: 543210 to complete transaction."))
        assertNull(DynamicTranslator.extractOtpFromText("Hello, how are you today?"))
    }

    @Test
    fun testTemplateCatalogPillConfigurations() {
        val boardingTemplate = IslandTemplateCatalog.find("tpl_boarding_pass")
        assertNotNull(boardingTemplate)
        assertEquals(PillRightDesign.BOARDING_PASS, boardingTemplate?.presentation?.pill?.rightDesign)

        val paymentTemplate = IslandTemplateCatalog.find("tpl_payment_wallet")
        assertNotNull(paymentTemplate)
        assertEquals(PillRightDesign.OTP_CODE, paymentTemplate?.presentation?.pill?.rightDesign)

        val fileTransferTemplate = IslandTemplateCatalog.find("tpl_file_transfer")
        assertNotNull(fileTransferTemplate)
        assertEquals(PillRightDesign.PROGRESS_PERCENT, fileTransferTemplate?.presentation?.pill?.rightDesign)
    }

    @Test
    fun testCompactPillConfigDefaults() {
        val config = CompactPillConfig()
        assertEquals(PillLeftDesign.ICON_AND_TEXT, config.leftDesign)
        assertEquals(PillRightDesign.AUTO, config.rightDesign)
        assertEquals(PillIconSource.DEFAULT, config.leftIconSource)
        assertEquals(PillTextSource.DEFAULT, config.leftTextSource)
        assertEquals(com.d4viddf.hyperbridge.models.translator.MediaCounterMode.ELAPSED, config.mediaCounterMode)
    }

    @Test
    fun testCompactPillConfigWithRemainingCounter() {
        val config = CompactPillConfig(
            mediaCounterMode = com.d4viddf.hyperbridge.models.translator.MediaCounterMode.REMAINING
        )
        assertEquals(com.d4viddf.hyperbridge.models.translator.MediaCounterMode.REMAINING, config.mediaCounterMode)
    }
}
