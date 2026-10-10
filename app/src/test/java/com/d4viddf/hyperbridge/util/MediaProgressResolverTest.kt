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
    }

    @Test
    fun extractsProgressFromOfText() {
        val res = MediaProgressResolver.extractProgressFromText("Playing track • 02:30 of 05:00")
        // 150 / 300 = 50%
        assertNotNull(res)
        assertEquals(50, res?.progressPercent)
        assertEquals("02:30", res?.currentFormatted)
        assertEquals("05:00", res?.durationFormatted)
    }

    @Test
    fun extractsProgressFromPipeText() {
        val res = MediaProgressResolver.extractProgressFromText("00:45 | 01:30")
        // 45 / 90 = 50%
        assertNotNull(res)
        assertEquals(50, res?.progressPercent)
    }

    @Test
    fun calculatesProgressCorrectly() {
        val res = MediaProgressResolver.calculateProgress(90_000L, 180_000L)
        assertNotNull(res)
        assertEquals(50, res?.progressPercent)
        assertEquals("01:30", res?.currentFormatted)
        assertEquals("03:00", res?.durationFormatted)
    }

    @Test
    fun handlesZeroDurationGracefully() {
        val res = MediaProgressResolver.calculateProgress(0L, 0L)
        assertNull(res)
    }
}
