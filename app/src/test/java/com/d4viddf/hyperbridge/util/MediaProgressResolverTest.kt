package com.d4viddf.hyperbridge.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MediaProgressResolverTest {

    @Test
    fun parsesTimeStringToSeconds() {
        assertEquals(83L, MediaProgressResolver.parseTimeStringToSeconds("01:23"))
        assertEquals(83L, MediaProgressResolver.parseTimeStringToSeconds("1:23"))
        assertEquals(225L, MediaProgressResolver.parseTimeStringToSeconds("03:45"))
        assertEquals(3665L, MediaProgressResolver.parseTimeStringToSeconds("01:01:05"))
        assertNull(MediaProgressResolver.parseTimeStringToSeconds("invalid"))
    }

    @Test
    fun formatsMillisToTime() {
        assertEquals("01:23", MediaProgressResolver.formatMillisToTime(83_000L))
        assertEquals("03:45", MediaProgressResolver.formatMillisToTime(225_000L))
        assertEquals("1:01:05", MediaProgressResolver.formatMillisToTime(3_665_000L))
    }

    @Test
    fun extractsProgressFromSlashText() {
        val res = MediaProgressResolver.extractProgressFromText("01:23 / 03:45")
        // 83 / 225 = ~36.88% -> 36%
        assertNotNull(res)
        assertEquals(36, res?.progressPercent)
        assertEquals("01:23", res?.currentFormatted)
        assertEquals("03:45", res?.durationFormatted)
        assertEquals("-02:22", res?.remainingFormatted)
    }

    @Test
    fun extractsProgressFromOfText() {
        val res = MediaProgressResolver.extractProgressFromText("Playing track • 02:30 of 05:00")
        // 150 / 300 = 50%
        assertNotNull(res)
        assertEquals(50, res?.progressPercent)
        assertEquals("02:30", res?.currentFormatted)
        assertEquals("05:00", res?.durationFormatted)
        assertEquals("-02:30", res?.remainingFormatted)
    }

    @Test
    fun extractsProgressFromPipeText() {
        val res = MediaProgressResolver.extractProgressFromText("00:45 | 01:30")
        // 45 / 90 = 50%
        assertNotNull(res)
        assertEquals(50, res?.progressPercent)
        assertEquals("-00:45", res?.remainingFormatted)
    }

    @Test
    fun calculatesProgressCorrectly() {
        val res = MediaProgressResolver.calculateProgress(90_000L, 180_000L)
        assertNotNull(res)
        assertEquals(50, res?.progressPercent)
        assertEquals("01:30", res?.currentFormatted)
        assertEquals("03:00", res?.durationFormatted)
        assertEquals("-01:30", res?.remainingFormatted)
    }

    @Test
    fun handlesZeroDurationGracefully() {
        val res = MediaProgressResolver.calculateProgress(0L, 0L)
        assertNull(res)
    }

    @Test
    fun extractsProgressFromSingleTimestamp() {
        val res = MediaProgressResolver.extractProgressFromText("02:45")
        assertNotNull(res)
        assertEquals(null, res?.progressPercent)
        assertEquals("02:45", res?.currentFormatted)
        assertEquals(null, res?.durationFormatted)
        assertEquals(165_000L, res?.currentMs)
    }

    @Test
    fun extractsProgressFromTextWithSingleTimestamp() {
        val res = MediaProgressResolver.extractProgressFromText("Episode 42 • 12:34")
        assertNotNull(res)
        assertEquals("12:34", res?.currentFormatted)
        assertEquals(754_000L, res?.currentMs)
    }

    @Test
    fun extractsProgressFromRemainingTimestamp() {
        val res = MediaProgressResolver.extractProgressFromText("-01:15")
        assertNotNull(res)
        assertEquals("-01:15", res?.remainingFormatted)
    }
}
