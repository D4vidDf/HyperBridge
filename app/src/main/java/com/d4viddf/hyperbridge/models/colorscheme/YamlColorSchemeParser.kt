package com.d4viddf.hyperbridge.models.colorscheme

import java.util.UUID

/**
 * Parser and serializer for human-readable YAML color scheme files (*.yaml, *.yml, *.hscheme.yaml).
 *
 * Example YAML:
 * ```yaml
 * id: sunset_neon
 * name: Sunset Neon
 * author: David
 * description: Vibrant warm palette
 * is_dark: true
 * roles:
 *   primary: "#FF5E3A"
 *   on_primary: "#FFFFFF"
 *   primary_container: "#5C1500"
 *   on_primary_container: "#FFDBCF"
 *   secondary: "#FF9500"
 *   surface: "#1A1616"
 * ```
 */
object YamlColorSchemeParser {

    /**
     * Parses a YAML string into a [ColorSchemeDefinition].
     */
    fun parse(yamlText: String): ColorSchemeDefinition {
        var id = UUID.randomUUID().toString()
        var name = "Custom Scheme"
        var author = "User"
        var description = ""
        var isDark = true
        val rolesMap = mutableMapOf<String, String>()

        var inRolesBlock = false

        yamlText.lineSequence().forEach { rawLine ->
            val line = rawLine.trimEnd()
            val trimmed = line.trim()

            // Skip comments and blank lines
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                return@forEach
            }

            if (trimmed.startsWith("roles:")) {
                inRolesBlock = true
                return@forEach
            }

            // Top-level key: value (when not indented)
            val isIndented = line.startsWith("  ") || line.startsWith("\t")

            if (!isIndented) {
                // Leaving roles block if encountering another top-level key
                inRolesBlock = false
                val colonIdx = trimmed.indexOf(':')
                if (colonIdx != -1) {
                    val key = trimmed.substring(0, colonIdx).trim().lowercase()
                    val value = cleanValue(trimmed.substring(colonIdx + 1).trim())
                    when (key) {
                        "id" -> if (value.isNotEmpty()) id = value
                        "name" -> if (value.isNotEmpty()) name = value
                        "author" -> if (value.isNotEmpty()) author = value
                        "description" -> description = value
                        "is_dark", "isdark", "dark" -> isDark = value.equals("true", ignoreCase = true) || value == "1"
                    }
                }
            } else if (inRolesBlock) {
                val colonIdx = trimmed.indexOf(':')
                if (colonIdx != -1) {
                    val rawRole = trimmed.substring(0, colonIdx).trim()
                    val rawVal = cleanValue(trimmed.substring(colonIdx + 1).trim())
                    val role = ColorSchemeRole.fromKey(rawRole)
                    if (role != null && rawVal.isNotEmpty()) {
                        rolesMap[role.tokenKey] = normalizeHex(rawVal)
                    } else if (rawRole.isNotEmpty() && rawVal.isNotEmpty()) {
                        rolesMap[rawRole] = normalizeHex(rawVal)
                    }
                }
            }
        }

        return ColorSchemeDefinition(
            id = id,
            name = name,
            author = author,
            description = description,
            isDark = isDark,
            roles = rolesMap
        )
    }

    /**
     * Serializes a [ColorSchemeDefinition] into clean, standard YAML.
     */
    fun serialize(def: ColorSchemeDefinition): String {
        val sb = StringBuilder()
        sb.appendLine("id: ${def.id}")
        sb.appendLine("name: ${escapeYaml(def.name)}")
        sb.appendLine("author: ${escapeYaml(def.author)}")
        if (def.description.isNotBlank()) {
            sb.appendLine("description: ${escapeYaml(def.description)}")
        }
        sb.appendLine("is_dark: ${def.isDark}")
        sb.appendLine()
        sb.appendLine("roles:")

        ColorSchemeRole.entries.forEach { role ->
            val hex = def.getHex(role)
            if (hex != null) {
                sb.appendLine("  ${role.tokenKey}: \"$hex\"")
            }
        }

        // Also serialize any extra custom roles not in standard enum
        def.roles.forEach { (key, hex) ->
            if (ColorSchemeRole.fromKey(key) == null) {
                sb.appendLine("  $key: \"$hex\"")
            }
        }

        return sb.toString()
    }

    private fun cleanValue(raw: String): String {
        var str = raw.trim()
        if ((str.startsWith("\"") && str.endsWith("\"")) || (str.startsWith("'") && str.endsWith("'"))) {
            if (str.length >= 2) {
                str = str.substring(1, str.length - 1)
            }
        }
        return str.trim()
    }

    private fun escapeYaml(value: String): String {
        return if (value.contains(":") || value.contains("#") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\\\"") + "\""
        } else {
            value
        }
    }

    private fun normalizeHex(hex: String): String {
        val clean = hex.trim().removePrefix("#")
        return when (clean.length) {
            3 -> "#${clean[0]}${clean[0]}${clean[1]}${clean[1]}${clean[2]}${clean[2]}".uppercase()
            6 -> "#${clean.uppercase()}"
            8 -> "#${clean.uppercase()}"
            else -> if (hex.startsWith("#")) hex else "#$hex"
        }
    }
}
