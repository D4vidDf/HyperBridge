package com.d4viddf.hyperbridge.models.colorscheme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YamlColorSchemeParserTest {

    @Test
    fun parseStandardYamlSuccessfully() {
        val yaml = """
            # Test color scheme
            id: sunset_neon
            name: Sunset Neon
            author: David
            description: A vibrant warm palette
            is_dark: true

            roles:
              primary: "#FF5E3A"
              on_primary: "#FFFFFF"
              primary_container: "#5C1500"
              on_primary_container: "#FFDBCF"
              secondary: "#FF9500"
              surface: "#1A1616"
              surface_container: "#241E1E"
        """.trimIndent()

        val parsed = YamlColorSchemeParser.parse(yaml)
        assertEquals("sunset_neon", parsed.id)
        assertEquals("Sunset Neon", parsed.name)
        assertEquals("David", parsed.author)
        assertEquals("A vibrant warm palette", parsed.description)
        assertTrue(parsed.isDark)

        assertEquals("#FF5E3A", parsed.getHex(ColorSchemeRole.PRIMARY))
        assertEquals("#FFFFFF", parsed.getHex(ColorSchemeRole.ON_PRIMARY))
        assertEquals("#5C1500", parsed.getHex(ColorSchemeRole.PRIMARY_CONTAINER))
        assertEquals("#FFDBCF", parsed.getHex(ColorSchemeRole.ON_PRIMARY_CONTAINER))
        assertEquals("#FF9500", parsed.getHex(ColorSchemeRole.SECONDARY))
        assertEquals("#1A1616", parsed.getHex(ColorSchemeRole.SURFACE))
        assertEquals("#241E1E", parsed.getHex(ColorSchemeRole.SURFACE_CONTAINER))
    }

    @Test
    fun roundTripSerializationMaintainsContent() {
        val original = ColorSchemeDefinition(
            id = "nord_frost",
            name = "Nord Frost",
            author = "Arctic",
            description = "Cool icy blue scheme",
            isDark = true,
            roles = mapOf(
                ColorSchemeRole.PRIMARY.tokenKey to "#88C0D0",
                ColorSchemeRole.ON_PRIMARY.tokenKey to "#2E3440",
                ColorSchemeRole.SECONDARY.tokenKey to "#81A1C1",
                ColorSchemeRole.SURFACE.tokenKey to "#2E3440",
                ColorSchemeRole.ON_SURFACE.tokenKey to "#ECEFF4"
            )
        )

        val yaml = YamlColorSchemeParser.serialize(original)
        val deserialized = YamlColorSchemeParser.parse(yaml)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.name, deserialized.name)
        assertEquals(original.author, deserialized.author)
        assertEquals(original.description, deserialized.description)
        assertEquals(original.isDark, deserialized.isDark)
        assertEquals("#88C0D0", deserialized.getHex(ColorSchemeRole.PRIMARY))
        assertEquals("#2E3440", deserialized.getHex(ColorSchemeRole.ON_PRIMARY))
        assertEquals("#81A1C1", deserialized.getHex(ColorSchemeRole.SECONDARY))
        assertEquals("#2E3440", deserialized.getHex(ColorSchemeRole.SURFACE))
        assertEquals("#ECEFF4", deserialized.getHex(ColorSchemeRole.ON_SURFACE))
    }
}
