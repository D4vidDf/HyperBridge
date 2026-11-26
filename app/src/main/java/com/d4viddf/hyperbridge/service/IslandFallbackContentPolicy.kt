package com.d4viddf.hyperbridge.service

/**
 * The plain title/text of a bridged notification only shows where SystemUI does not draw it as an
 * Island: when HyperOS refuses the focus notification (for example the XMS auth "scope mismatch"
 * on CN ROMs in #485, where FocusPlugin drops the Island a couple of seconds after posting), on
 * devices without the Island, and in anything that reads the notification directly, such as a
 * paired watch.
 *
 * With the focus permission granted, tapping that entry opens the source app, so it repeats the
 * source's own title and text instead of "Failed to display notification island. Tap for
 * diagnostics.". The generic text stays for the missing-permission case, where the tap does open
 * the troubleshooting screen, and when the source has nothing to show.
 */
object IslandFallbackContentPolicy {
    fun showsSourceContent(hasFocusPermission: Boolean, title: String, text: String): Boolean =
        hasFocusPermission && (title.isNotBlank() || text.isNotBlank())
}
