package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.components.ListOptionCard
import com.d4viddf.hyperbridge.ui.screens.theme.ShapeStyle
import com.d4viddf.hyperbridge.ui.screens.theme.getExpressiveShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSettingsScreen(
    onBack: () -> Unit,
    onNavSettingsClick: () -> Unit,
    onIslandSettingsClick: () -> Unit,
    onEngineSettingsClick: () -> Unit
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
        Column(modifier = Modifier
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
        ) {
            // Island Settings Card
            ListOptionCard(
                title = stringResource(R.string.engine),
                subtitle = stringResource(R.string.engine_desc),
                icon = Icons.Outlined.Memory,
                shape = getExpressiveShape(3, 0, ShapeStyle.Large),
                onClick = onEngineSettingsClick
            )
            Spacer(Modifier.height(2.dp))
            ListOptionCard(
                title = stringResource(R.string.island_behavior_title),
                subtitle = stringResource(R.string.island_behavior_desc),
                icon = Icons.Outlined.DisplaySettings,
                shape = getExpressiveShape(3, 1, ShapeStyle.Large),
                onClick = onIslandSettingsClick
            )
            Spacer(Modifier.height(2.dp))
            ListOptionCard(
                title = stringResource(R.string.nav_layout_title),
                subtitle = stringResource(R.string.nav_layout_desc),
                icon = Icons.Outlined.Navigation,
                shape = getExpressiveShape(3, 2, ShapeStyle.Large),
                onClick = onNavSettingsClick
            )
        }
    }
}