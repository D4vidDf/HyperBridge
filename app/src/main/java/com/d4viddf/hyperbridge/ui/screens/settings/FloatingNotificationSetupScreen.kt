package com.d4viddf.hyperbridge.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.service.floating.FloatingNotificationSetup
import com.d4viddf.hyperbridge.service.floating.FloatingSetupStatus
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme
import com.d4viddf.hyperbridge.util.DeviceUtils
import com.d4viddf.hyperbridge.util.NotificationSettingsNavigator
import com.d4viddf.hyperbridge.util.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FloatingSetupAppItem(
    val packageName: String,
    val label: String,
    val status: FloatingSetupStatus,
    val isConfirmed: Boolean,
    val previewIcon: ImageBitmap? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingNotificationSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context.applicationContext) }
    val selectedPackages by preferences.allowedPackagesFlow.collectAsState(initial = emptySet())
    val confirmedPackages by preferences.floatingSetupConfirmedPackagesFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    val requiresManualSetup = remember { DeviceUtils.isXiaomi && DeviceUtils.isCompatibleOS() }

    LaunchedEffect(Unit) {
        preferences.setFloatingSetupNoticePending(false)
    }

    val appItems = remember(selectedPackages, confirmedPackages, requiresManualSetup) {
        selectedPackages.sorted().map { packageName ->
            val label = try {
                val info = context.packageManager.getApplicationInfo(packageName, 0)
                context.packageManager.getApplicationLabel(info).toString()
            } catch (_: Exception) {
                packageName
            }
            val status = FloatingNotificationSetup.status(
                isSelected = true,
                userConfirmedDisabled = packageName in confirmedPackages,
                requiresManualSetup = requiresManualSetup
            )
            FloatingSetupAppItem(
                packageName = packageName,
                label = label,
                status = status,
                isConfirmed = packageName in confirmedPackages
            )
        }
    }

    FloatingNotificationSetupContent(
        apps = appItems,
        requiresManualSetup = requiresManualSetup,
        onBack = onBack,
        onOpenSettings = { packageName ->
            if (!NotificationSettingsNavigator.openForApp(context, packageName)) {
                Toast.makeText(context, R.string.settings_not_available, Toast.LENGTH_SHORT).show()
            }
        },
        onConfirmedChange = { packageName, confirmed ->
            scope.launch { preferences.setFloatingSetupConfirmed(packageName, confirmed) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingNotificationSetupContent(
    apps: List<FloatingSetupAppItem>,
    requiresManualSetup: Boolean,
    onBack: () -> Unit,
    onOpenSettings: (packageName: String) -> Unit,
    onConfirmedChange: (packageName: String, confirmed: Boolean) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.floating_setup_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.floating_setup_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.floating_setup_settings_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.floating_setup_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = stringResource(R.string.floating_setup_limitation),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Alternative recommendation banner
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.floating_setup_alternative_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.floating_setup_alternative_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (apps.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.floating_setup_no_apps_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.floating_setup_no_apps),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.diagnostic_selected_apps),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val confirmedCount = apps.count { it.isConfirmed }
                        Surface(
                            shape = CircleShape,
                            color = if (confirmedCount == apps.size && apps.isNotEmpty()) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        ) {
                            Text(
                                text = "$confirmedCount / ${apps.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (confirmedCount == apps.size && apps.isNotEmpty()) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            items(apps, key = { it.packageName }) { app ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconImage(
                                packageName = app.packageName,
                                previewBitmap = app.previewIcon,
                                modifier = Modifier.size(52.dp)
                            )

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(6.dp))
                                StatusBadge(status = app.status)
                            }
                        }

                        FilledTonalButton(
                            onClick = { onOpenSettings(app.packageName) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.open_notification_settings),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        if (requiresManualSetup) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onConfirmedChange(app.packageName, !app.isConfirmed) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = app.isConfirmed,
                                        onCheckedChange = { checked ->
                                            onConfirmedChange(app.packageName, checked)
                                        }
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.floating_disabled_confirmation),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: FloatingSetupStatus) {
    val icon = when (status) {
        FloatingSetupStatus.USER_CONFIRMED, FloatingSetupStatus.NOT_REQUIRED -> Icons.Default.CheckCircle
        FloatingSetupStatus.NEEDS_REVIEW -> Icons.Default.Warning
    }
    val labelRes = when (status) {
        FloatingSetupStatus.USER_CONFIRMED -> R.string.floating_status_user_confirmed
        FloatingSetupStatus.NEEDS_REVIEW -> R.string.floating_status_needs_review
        FloatingSetupStatus.NOT_REQUIRED -> R.string.floating_status_not_required
    }
    val containerColor = when (status) {
        FloatingSetupStatus.USER_CONFIRMED -> MaterialTheme.colorScheme.primaryContainer
        FloatingSetupStatus.NEEDS_REVIEW -> MaterialTheme.colorScheme.tertiaryContainer
        FloatingSetupStatus.NOT_REQUIRED -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = when (status) {
        FloatingSetupStatus.USER_CONFIRMED -> MaterialTheme.colorScheme.onPrimaryContainer
        FloatingSetupStatus.NEEDS_REVIEW -> MaterialTheme.colorScheme.onTertiaryContainer
        FloatingSetupStatus.NOT_REQUIRED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AppIconImage(
    packageName: String,
    modifier: Modifier = Modifier,
    previewBitmap: ImageBitmap? = null
) {
    val context = LocalContext.current
    val iconBitmap by produceState<ImageBitmap?>(initialValue = previewBitmap, packageName) {
        if (previewBitmap != null) {
            value = previewBitmap
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            try {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                drawable.toBitmap().asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    val currentBitmap = iconBitmap
    if (currentBitmap != null) {
        Image(
            bitmap = currentBitmap,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(14.dp))
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FloatingNotificationSetupScreenPreview() {
    HyperBridgeTheme {
        FloatingNotificationSetupContent(
            apps = listOf(
                FloatingSetupAppItem(
                    packageName = "com.whatsapp",
                    label = "WhatsApp",
                    status = FloatingSetupStatus.NEEDS_REVIEW,
                    isConfirmed = false
                ),
                FloatingSetupAppItem(
                    packageName = "org.telegram.messenger",
                    label = "Telegram",
                    status = FloatingSetupStatus.USER_CONFIRMED,
                    isConfirmed = true
                ),
                FloatingSetupAppItem(
                    packageName = "com.google.android.gm",
                    label = "Gmail",
                    status = FloatingSetupStatus.NOT_REQUIRED,
                    isConfirmed = false
                )
            ),
            requiresManualSetup = true,
            onBack = {},
            onOpenSettings = {},
            onConfirmedChange = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FloatingNotificationSetupScreenEmptyPreview() {
    HyperBridgeTheme {
        FloatingNotificationSetupContent(
            apps = emptyList(),
            requiresManualSetup = true,
            onBack = {},
            onOpenSettings = {},
            onConfirmedChange = { _, _ -> }
        )
    }
}

