package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.ui.components.IslandSettingsControl
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme
import kotlinx.coroutines.launch

@Composable
fun IslandSettingsScreen(
    onBack: () -> Unit,
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    val globalConfig by preferences.globalConfigFlow.collectAsState(initial = IslandConfig(
        isFloat = false,
        isShowShade = false,
        timeout = 10
    ))

    IslandSettingsContent(
        globalConfig = globalConfig,
        onBack = onBack,
        onUpdateConfig = { newConfig ->
            scope.launch { preferences.updateGlobalConfig(newConfig) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandSettingsContent(
    globalConfig: IslandConfig,
    onBack: () -> Unit,
    onUpdateConfig: (IslandConfig) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.global_settings)) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            IslandSettingsControl(
                config = globalConfig,
                onUpdate = onUpdateConfig
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun IslandSettingsScreenPreview() {
    HyperBridgeTheme {
        IslandSettingsContent(
            globalConfig = IslandConfig(isFloat = true, isShowShade = true, timeout = 5, floatTimeout = 6),
            onBack = {},
            onUpdateConfig = {}
        )
    }
}
