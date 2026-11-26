package com.d4viddf.hyperbridge.service.translators

import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Test

class TextPercentageExtractorTest {

    @After
    fun resetCustomKeywords() {
        TextPercentageExtractor.customKeywords = emptySet()
    }

    @Test
    fun readsDownloadProgress() {
        assertEquals(42, TextPercentageExtractor.extract("Downloading", "file.zip • 42%"))
        assertEquals(7, TextPercentageExtractor.extract("Installing 7 %", null))
        assertEquals(100, TextPercentageExtractor.extract(null, "Descargando 100%"))
    }

    @Test
    fun textWinsOverTitle() {
        assertEquals(30, TextPercentageExtractor.extract("Update 10%", "30%"))
    }

    @Test
    fun ignoresValuesAboveHundred() {
        assertNull(TextPercentageExtractor.extract(null, "Battery saved 150%"))
    }

    @Test
    fun percentOffIsNotProgress() {
        assertNull(TextPercentageExtractor.extract("Big summer event", "50% off all games"))
        assertNull(TextPercentageExtractor.extract(null, "Up to 70 % OFF"))
    }

    @Test
    fun signedPercentIsNotProgress() {
        assertNull(TextPercentageExtractor.extract(null, "Flash sale -20% today"))
        assertNull(TextPercentageExtractor.extract(null, "Precio −15 %"))
    }

    @Test
    fun discountWordsSkipTheString() {
        assertNull(TextPercentageExtractor.extract(null, "Save 30% on your next order"))
        assertNull(TextPercentageExtractor.extract(null, "20% de descuento en la Play Store"))
        assertNull(TextPercentageExtractor.extract(null, "Rebajas: hasta 40%"))
        assertNull(TextPercentageExtractor.extract(null, "15% Rabatt auf alles"))
        assertNull(TextPercentageExtractor.extract(null, "Скидка 25%"))
    }

    @Test
    fun discountInTitleFallsBackToNothingButTextStillCounts() {
        assertNull(TextPercentageExtractor.extract("50% off", null))
        assertEquals(60, TextPercentageExtractor.extract("50% off", "Downloading 60%"))
    }

    @Test
    fun discountWordsMatchAsWholeWordsOnly() {
        // "saved", "offline", "salesforce" are not discount words
        assertEquals(55, TextPercentageExtractor.extract(null, "Saved 55%"))
        assertEquals(12, TextPercentageExtractor.extract(null, "12% offline maps"))
        assertEquals(80, TextPercentageExtractor.extract(null, "Salesforce update 80%"))
    }

    @Test
    fun skipsADiscountMatchAndKeepsALaterProgressMatch() {
        assertEquals(35, TextPercentageExtractor.extract(null, "-10% bundle · installing 35%"))
    }

    @Test
    fun customKeywordsSkipTheString() {
        assertEquals(25, TextPercentageExtractor.extract(null, "Liquidación 25% en tienda"))
        TextPercentageExtractor.customKeywords = setOf(" liquidación ", "")
        assertNull(TextPercentageExtractor.extract(null, "LIQUIDACIÓN 25% en tienda"))
        assertEquals(40, TextPercentageExtractor.extract(null, "Downloading 40%"))
    }

    @Test
    fun customKeywordsMatchWholeWordsLiterally() {
        TextPercentageExtractor.customKeywords = setOf("bonus", "x2.5")
        assertEquals(60, TextPercentageExtractor.extract(null, "Bonuses unlocked 60%"))
        assertNull(TextPercentageExtractor.extract(null, "Bonus 60% extra"))
        assertEquals(10, TextPercentageExtractor.extract(null, "x205 10%"))
        assertNull(TextPercentageExtractor.extract(null, "x2.5 points 10%"))
    }

    @Test
    fun clearingCustomKeywordsRestoresProgress() {
        TextPercentageExtractor.customKeywords = setOf("bonus")
        TextPercentageExtractor.customKeywords = emptySet()
        assertEquals(60, TextPercentageExtractor.extract(null, "Bonus 60%"))
    }
}
