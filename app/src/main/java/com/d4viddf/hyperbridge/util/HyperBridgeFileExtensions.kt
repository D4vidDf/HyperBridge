package com.d4viddf.hyperbridge.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.util.Locale

/**
 * Defines and validates official HyperBridge file extensions for imports and exports.
 */
object HyperBridgeFileExtensions {
    const val EXT_BACKUP = "hbr"
    const val EXT_THEME = "htheme"
    const val EXT_TRANSLATOR = "htrans"
    const val EXT_WIDGET = "hwidget"
    const val EXT_SCHEME = "hscheme"

    val SUPPORTED_EXTENSIONS: Set<String> = setOf(
        EXT_BACKUP,
        EXT_THEME,
        EXT_TRANSLATOR,
        EXT_WIDGET,
        EXT_SCHEME
    )

    /**
     * Extracts the lowercase file extension (without the leading dot) from a file name or path,
     * or returns null if the name has no extension.
     */
    fun extractExtension(fileName: String?): String? {
        if (fileName.isNullOrBlank()) return null
        val simpleName = fileName.replace('\\', '/').substringAfterLast('/')
        val dotIndex = simpleName.lastIndexOf('.')
        if (dotIndex <= 0 || dotIndex == simpleName.lastIndex) return null
        return simpleName.substring(dotIndex + 1).lowercase(Locale.ROOT)
    }

    /**
     * Returns true if [fileName] ends with one of the official HyperBridge extensions:
     * `.hbr`, `.htheme`, `.htrans`, `.hwidget`, or `.hscheme`.
     */
    fun isSupportedPackageFile(fileName: String?): Boolean {
        val ext = extractExtension(fileName) ?: return false
        return ext in SUPPORTED_EXTENSIONS
    }

    /**
     * Returns true if [fileName] has the `.hbr` backup extension.
     */
    fun isBackupFile(fileName: String?): Boolean {
        return extractExtension(fileName) == EXT_BACKUP
    }

    /**
     * Returns true if [fileName] has the `.htheme` (or legacy `.hbr`) theme extension.
     */
    fun isThemeFile(fileName: String?): Boolean {
        val ext = extractExtension(fileName) ?: return false
        return ext == EXT_THEME || ext == EXT_BACKUP
    }

    /**
     * Returns true if [fileName] has the `.hscheme` color scheme extension.
     */
    fun isColorSchemeFile(fileName: String?): Boolean {
        return extractExtension(fileName) == EXT_SCHEME
    }

    /**
     * Resolves the display file name from a [Uri] using [OpenableColumns.DISPLAY_NAME]
     * with a fallback to the URI's path segment.
     */
    fun resolveFileName(context: Context, uri: Uri): String? {
        if (uri.scheme.equals("content", ignoreCase = true)) {
            runCatching {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) {
                            val displayName = cursor.getString(index)
                            if (!displayName.isNullOrBlank()) {
                                return displayName
                            }
                        }
                    }
                }
            }
        }
        return uri.lastPathSegment
            ?.replace('\\', '/')
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
    }
}
