package com.d4viddf.hyperbridge.ui.screens.theme.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.components.EngineOptionCard
import com.d4viddf.hyperbridge.ui.components.EnginePreview

@Composable
fun EngineThemeContent(
    isNative: Boolean,
    onEngineChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.engine_preview_title), // Extracted
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        EnginePreview(isNative = isNative)

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.engine_design_section), // Extracted
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        EngineOptionCard(
            title = stringResource(R.string.engine_xiaomi_title), // Extracted
            description = stringResource(R.string.engine_xiaomi_desc), // Extracted
            isSelected = !isNative,
            onClick = { onEngineChange(false) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        EngineOptionCard(
            title = stringResource(R.string.engine_native_title), // Extracted
            description = stringResource(R.string.engine_native_desc), // Extracted
            isSelected = isNative,
            onClick = { onEngineChange(true) }
        )
    }
}