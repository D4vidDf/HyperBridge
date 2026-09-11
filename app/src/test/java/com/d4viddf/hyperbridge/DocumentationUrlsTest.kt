package com.d4viddf.hyperbridge

import com.d4viddf.hyperbridge.util.DocumentationUrls
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DocumentationUrlsTest {

    @Test
    fun testDocumentationConstants() {
        assertEquals("https://hyper-bridge.app", DocumentationUrls.BASE_URL)
        assertEquals("https://hyper-bridge.app/docs/", DocumentationUrls.DOCS)
        assertEquals("https://hyper-bridge.app/docs/customization/", DocumentationUrls.CUSTOMIZATION_DOCS)
        assertEquals("https://hyper-bridge.app/docs/customization/theme-creator/", DocumentationUrls.THEME_CREATOR_DOCS)
    }

    @Test
    fun testChangelogUrlDefault() {
        val url = DocumentationUrls.getChangelogUrl(Locale.ENGLISH)
        assertEquals("https://hyper-bridge.app/changelog/", url)
    }

    @Test
    fun testChangelogUrlSpanish() {
        val urlEs = DocumentationUrls.getChangelogUrl(Locale.forLanguageTag("es-ES"))
        assertEquals("https://hyper-bridge.app/es/changelog/", urlEs)

        val urlEsLa = DocumentationUrls.getChangelogUrl(Locale.forLanguageTag("es-419"))
        assertEquals("https://hyper-bridge.app/es-419/changelog/", urlEsLa)

        val urlEsMx = DocumentationUrls.getChangelogUrl(Locale.forLanguageTag("es-MX"))
        assertEquals("https://hyper-bridge.app/es-419/changelog/", urlEsMx)
    }

    @Test
    fun testChangelogUrlVariousLanguages() {
        assertEquals("https://hyper-bridge.app/de/changelog/", DocumentationUrls.getChangelogUrl(Locale.GERMAN))
        assertEquals("https://hyper-bridge.app/fr/changelog/", DocumentationUrls.getChangelogUrl(Locale.FRENCH))
        assertEquals("https://hyper-bridge.app/it/changelog/", DocumentationUrls.getChangelogUrl(Locale.ITALIAN))
        assertEquals("https://hyper-bridge.app/ja/changelog/", DocumentationUrls.getChangelogUrl(Locale.JAPANESE))
        assertEquals("https://hyper-bridge.app/ko/changelog/", DocumentationUrls.getChangelogUrl(Locale.KOREAN))
        assertEquals("https://hyper-bridge.app/ru/changelog/", DocumentationUrls.getChangelogUrl(Locale.forLanguageTag("ru")))
        assertEquals("https://hyper-bridge.app/pt-br/changelog/", DocumentationUrls.getChangelogUrl(Locale.forLanguageTag("pt-BR")))
        assertEquals("https://hyper-bridge.app/zh-tw/changelog/", DocumentationUrls.getChangelogUrl(Locale.forLanguageTag("zh-TW")))
        assertEquals("https://hyper-bridge.app/id/changelog/", DocumentationUrls.getChangelogUrl(Locale.forLanguageTag("id-ID")))
    }
}
