package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.util.toBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(
    onBack: () -> Unit,
    onSetupClick: () -> Unit,
    onLicensesClick: () -> Unit,
    onBehaviorClick: () -> Unit,
    onGlobalSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit // NEW
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()

    val appVersion = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) { "1.0.0" }

    val appIconBitmap = remember(context) {
        try { context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap() } catch (e: Exception) { null }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HEADER ---
            Spacer(modifier = Modifier.height(16.dp))

            if (appIconBitmap != null) {
                Image(
                    bitmap = appIconBitmap,
                    contentDescription = stringResource(R.string.logo_desc),
                    modifier = Modifier.size(96.dp).padding(bottom = 12.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = stringResource(R.string.logo_desc),
                    modifier = Modifier
                        .size(80.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .padding(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            Text(
                text = stringResource(R.string.developer_credit),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.version_template, appVersion),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- GROUP 1: CONFIGURATION ---
            SettingsGroupTitle(stringResource(R.string.group_configuration))
            SettingsGroupCard {
                // System Setup
                SettingsItem(
                    icon = Icons.Default.SettingsSuggest,
                    title = stringResource(R.string.system_setup),
                    subtitle = stringResource(R.string.system_setup_subtitle),
                    onClick = onSetupClick
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))

                // Island Behavior
                SettingsItem(
                    icon = Icons.Default.Tune,
                    title = stringResource(R.string.island_behavior),
                    subtitle = stringResource(R.string.limit_strategy),
                    onClick = onBehaviorClick
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))

                // Global Appearance
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.global_settings),
                    subtitle = stringResource(R.string.island_appearance),
                    onClick = onGlobalSettingsClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- GROUP 2: ABOUT ---
            SettingsGroupTitle(stringResource(R.string.group_about))
            SettingsGroupCard {
                // Developer
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = stringResource(R.string.developer),
                    subtitle = stringResource(R.string.developer_subtitle),
                    onClick = { uriHandler.openUri("https://d4viddf.com") }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))

                // Version History (NEW)
                SettingsItem(
                    icon = Icons.Default.History,
                    title = stringResource(R.string.version_history),
                    subtitle = "0.1.0 - 0.2.0",
                    onClick = onHistoryClick
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))

                // Source Code
                SettingsItem(
                    icon = Icons.Default.Code,
                    title = stringResource(R.string.source_code),
                    subtitle = stringResource(R.string.source_code_subtitle),
                    onClick = { uriHandler.openUri("https://github.com/D4vidDf/HyperBridge") }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))

                // Licenses
                SettingsItem(
                    icon = Icons.Default.Description,
                    title = stringResource(R.string.licenses),
                    subtitle = stringResource(R.string.licenses_subtitle),
                    onClick = onLicensesClick
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(stringResource(R.string.footer_made_with_love), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsGroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}