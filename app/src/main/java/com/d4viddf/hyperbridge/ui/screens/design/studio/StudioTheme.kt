package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Material Expressive Theme scope applied exclusively to the Studio Design micro-widget builder.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StudioExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = run {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        content = content
    )
}
