package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable

/**
 * Material Expressive Theme scope applied exclusively to the Studio Design micro-widget builder.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StudioExpressiveTheme(
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        content = content
    )
}
