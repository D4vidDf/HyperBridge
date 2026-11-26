package com.d4viddf.hyperbridge.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HyperBridgeFileExtensionsTest {

    @Test
    fun `extractExtension returns lowercase extension or null when missing`() {
        assertEquals("hbr", HyperBridgeFileExtensions.extractExtension("HyperBridge_Backup_20261030.hbr"))
        assertEquals("htheme", HyperBridgeFileExtensions.extractExtension("dark_island_v1.HTHEME"))
        assertEquals("htrans", HyperBridgeFileExtensions.extractExtension("/storage/emulated/0/Download/spotify.v1.htrans"))
        assertEquals("hwidget", HyperBridgeFileExtensions.extractExtension("C:\\Exports\\media_player.hwidget"))
        assertEquals("hscheme", HyperBridgeFileExtensions.extractExtension("amoled_neon.hscheme"))
        assertNull(HyperBridgeFileExtensions.extractExtension("README"))
        assertNull(HyperBridgeFileExtensions.extractExtension(".hidden"))
        assertNull(HyperBridgeFileExtensions.extractExtension("trailingdot."))
        assertNull(HyperBridgeFileExtensions.extractExtension(""))
        assertNull(HyperBridgeFileExtensions.extractExtension(null))
    }

    @Test
    fun `isSupportedPackageFile accepts only HyperBridge extensions`() {
        assertTrue(HyperBridgeFileExtensions.isSupportedPackageFile("backup.hbr"))
        assertTrue(HyperBridgeFileExtensions.isSupportedPackageFile("theme_v1.htheme"))
        assertTrue(HyperBridgeFileExtensions.isSupportedPackageFile("translator.htrans"))
        assertTrue(HyperBridgeFileExtensions.isSupportedPackageFile("widget.hwidget"))
        assertTrue(HyperBridgeFileExtensions.isSupportedPackageFile("palette.hscheme"))
        assertTrue(HyperBridgeFileExtensions.isSupportedPackageFile("PALETTE.HSCHEME"))

        assertFalse(HyperBridgeFileExtensions.isSupportedPackageFile("archive.zip"))
        assertFalse(HyperBridgeFileExtensions.isSupportedPackageFile("data.json"))
        assertFalse(HyperBridgeFileExtensions.isSupportedPackageFile("config.yaml"))
        assertFalse(HyperBridgeFileExtensions.isSupportedPackageFile("document.pdf"))
        assertFalse(HyperBridgeFileExtensions.isSupportedPackageFile("app.apk"))
        assertFalse(HyperBridgeFileExtensions.isSupportedPackageFile("no_extension"))
    }

    @Test
    fun `specific extension helpers validate matching formats`() {
        assertTrue(HyperBridgeFileExtensions.isBackupFile("HyperBridge_Backup.hbr"))
        assertFalse(HyperBridgeFileExtensions.isBackupFile("theme.htheme"))

        assertTrue(HyperBridgeFileExtensions.isThemeFile("theme.htheme"))
        assertTrue(HyperBridgeFileExtensions.isThemeFile("legacy_theme.hbr"))
        assertFalse(HyperBridgeFileExtensions.isThemeFile("widget.hwidget"))

        assertTrue(HyperBridgeFileExtensions.isColorSchemeFile("ocean.hscheme"))
        assertFalse(HyperBridgeFileExtensions.isColorSchemeFile("ocean.yaml"))
    }
}
