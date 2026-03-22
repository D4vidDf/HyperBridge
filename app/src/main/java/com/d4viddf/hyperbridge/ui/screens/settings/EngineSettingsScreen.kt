package com.d4viddf.hyperbridge.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.components.EngineOptionCard
import com.d4viddf.hyperbridge.ui.components.EnginePreview
import androidx.core.content.edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngineSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("hyperbridge_settings", Context.MODE_PRIVATE)
    var isNative by remember { mutableStateOf(prefs.getBoolean("use_native_live_updates", false)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.engine_config_title)) }, // Extracted
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.engine_section_preview), // Extracted
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            EnginePreview(isNative = isNative)

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.engine_section_design), // Extracted
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            EngineOptionCard(
                title = stringResource(R.string.engine_xiaomi_title), // Extracted
                description = stringResource(R.string.engine_xiaomi_desc), // Extracted
                isSelected = !isNative,
                onClick = {
                    isNative = false
                    prefs.edit { putBoolean("use_native_live_updates", false) }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            EngineOptionCard(
                title = stringResource(R.string.engine_native_title), // Extracted
                description = stringResource(R.string.engine_native_desc), // Extracted
                isSelected = isNative,
                onClick = {
                    isNative = true
                    prefs.edit { putBoolean("use_native_live_updates", true) }
                }
            )
        }
    }
}