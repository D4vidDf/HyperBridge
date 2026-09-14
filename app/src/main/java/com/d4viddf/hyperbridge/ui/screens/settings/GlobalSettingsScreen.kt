package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.components.ListOptionCard
import com.d4viddf.hyperbridge.ui.screens.theme.ShapeStyle
import com.d4viddf.hyperbridge.ui.screens.theme.getExpressiveShape
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSettingsScreen(
    onBack: () -> Unit,
    onNavSettingsClick: () -> Unit,
    onInlineReplyClick: () -> Unit,
    onIslandSettingsClick: () -> Unit,
    onEngineSettingsClick: () -> Unit,
    onDndSettingsClick: () -> Unit,
    onPermanentIslandClick: () -> Unit,
    onSmartActionsClick: () -> Unit
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
            // Group 1: Engine, Island Behaviour, Do Not Disturb
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
                title = stringResource(R.string.dnd_mode_title),
                subtitle = stringResource(R.string.dnd_mode_desc),
                icon = Icons.Outlined.DoNotDisturbOn,
                shape = getExpressiveShape(3, 2, ShapeStyle.Large),
                onClick = onDndSettingsClick
            )
            Spacer(Modifier.height(16.dp))

            // Group 2: Permanent Island and Smart Actions
            ListOptionCard(
                title = stringResource(R.string.permanent_island_title),
                subtitle = stringResource(R.string.permanent_island_desc),
                icon = Icons.Outlined.PushPin,
                shape = getExpressiveShape(2, 0, ShapeStyle.Large),
                onClick = onPermanentIslandClick
            )
            Spacer(Modifier.height(2.dp))
            ListOptionCard(
                title = stringResource(R.string.smart_actions_title),
                subtitle = stringResource(R.string.smart_actions_desc),
                icon = Icons.Outlined.AutoAwesome,
                shape = getExpressiveShape(2, 1, ShapeStyle.Large),
                onClick = onSmartActionsClick
            )
            Spacer(Modifier.height(16.dp))

            // Group 3: Navigation Design and Inline Reply
            ListOptionCard(
                title = stringResource(R.string.nav_layout_title),
                subtitle = stringResource(R.string.nav_layout_desc),
                icon = Icons.Outlined.Navigation,
                shape = getExpressiveShape(2, 0, ShapeStyle.Large),
                onClick = onNavSettingsClick
            )
            Spacer(Modifier.height(2.dp))
            ListOptionCard(
                title = stringResource(R.string.inline_reply_title),
                subtitle = stringResource(R.string.customize_inline_reply),
                icon = Icons.Outlined.Edit,
                shape = getExpressiveShape(2, 1, ShapeStyle.Large),
                onClick = onInlineReplyClick
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GlobalSettingsScreenPreview() {
    HyperBridgeTheme {
        GlobalSettingsScreen(
            onBack = {},
            onNavSettingsClick = {},
            onInlineReplyClick = {},
            onIslandSettingsClick = {},
            onEngineSettingsClick = {},
            onDndSettingsClick = {},
            onPermanentIslandClick = {},
            onSmartActionsClick = {}
        )
    }
}