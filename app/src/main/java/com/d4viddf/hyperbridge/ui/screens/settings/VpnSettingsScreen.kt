package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.service.vpn.VpnDetectionMode
import com.d4viddf.hyperbridge.service.vpn.VpnProviderRegistry
import com.d4viddf.hyperbridge.util.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class VpnProviderItemInfo(
    val name: String,
    val packageName: String,
    val enabled: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }
    val registry = remember { VpnProviderRegistry(context) }

    val detectionMode by preferences.vpnDetectionModeFlow.collectAsState(
        initial = VpnDetectionMode.AUTO
    )
    val disabledProviders by preferences.vpnDisabledProvidersFlow.collectAsState(
        initial = emptySet()
    )
    val priorityOrder by preferences.vpnProviderPriorityFlow.collectAsState(
        initial = emptyList()
    )

    val providerItems = remember { mutableStateListOf<VpnProviderItemInfo>() }
    var isLoaded by remember { mutableStateOf(false) }
    var showModeSheet by remember { mutableStateOf(false) }

    LaunchedEffect(disabledProviders, priorityOrder) {
        withContext(Dispatchers.IO) {
            val installedPackages = registry.providerPackages(refresh = true)
            val ordered = priorityOrder.filter { it in installedPackages }
            val remaining = installedPackages
                .filter { it !in ordered }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { registry.appLabel(it) })
            val finalOrder = ordered + remaining

            val list = finalOrder.map { pkg ->
                VpnProviderItemInfo(
                    name = registry.appLabel(pkg),
                    packageName = pkg,
                    enabled = pkg !in disabledProviders
                )
            }

            withContext(Dispatchers.Main) {
                providerItems.clear()
                providerItems.addAll(list)
                isLoaded = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vpn_customization_title)) },
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
                stringResource(R.string.group_configuration),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Detection Mode Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showModeSheet = true }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.vpn_detection_mode_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = when (detectionMode) {
                                VpnDetectionMode.AUTO -> stringResource(R.string.vpn_detection_mode_auto)
                                VpnDetectionMode.SYSTEM_ONLY -> stringResource(R.string.vpn_detection_mode_system_only)
                                VpnDetectionMode.NOTIFICATIONS_ONLY -> stringResource(R.string.vpn_detection_mode_notifications_only)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = when (detectionMode) {
                                VpnDetectionMode.AUTO -> stringResource(R.string.vpn_detection_mode_auto_desc)
                                VpnDetectionMode.SYSTEM_ONLY -> stringResource(R.string.vpn_detection_mode_system_only_desc)
                                VpnDetectionMode.NOTIFICATIONS_ONLY -> stringResource(R.string.vpn_detection_mode_notifications_only_desc)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                stringResource(R.string.vpn_providers_section_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.vpn_providers_section_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!isLoaded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (providerItems.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.vpn_no_providers_installed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    providerItems.forEachIndexed { index, item ->
                        VpnProviderPriorityRow(
                            rank = index + 1,
                            item = item,
                            isFirst = index == 0,
                            isLast = index == providerItems.lastIndex,
                            onMoveUp = {
                                if (index > 0) {
                                    providerItems.add(index - 1, providerItems.removeAt(index))
                                    scope.launch {
                                        preferences.setVpnProviderPriority(providerItems.map { it.packageName })
                                    }
                                }
                            },
                            onMoveDown = {
                                if (index < providerItems.lastIndex) {
                                    providerItems.add(index + 1, providerItems.removeAt(index))
                                    scope.launch {
                                        preferences.setVpnProviderPriority(providerItems.map { it.packageName })
                                    }
                                }
                            },
                            onToggle = { enabled ->
                                providerItems[index] = item.copy(enabled = enabled)
                                scope.launch {
                                    preferences.setVpnProviderEnabled(item.packageName, enabled)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showModeSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showModeSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.vpn_detection_mode_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                VpnDetectionMode.entries.forEach { mode ->
                    val isSelected = mode == detectionMode
                    Surface(
                        onClick = {
                            scope.launch { preferences.setVpnDetectionMode(mode) }
                            showModeSheet = false
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            Color.Transparent
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (mode) {
                                        VpnDetectionMode.AUTO -> stringResource(R.string.vpn_detection_mode_auto)
                                        VpnDetectionMode.SYSTEM_ONLY -> stringResource(R.string.vpn_detection_mode_system_only)
                                        VpnDetectionMode.NOTIFICATIONS_ONLY -> stringResource(R.string.vpn_detection_mode_notifications_only)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = when (mode) {
                                        VpnDetectionMode.AUTO -> stringResource(R.string.vpn_detection_mode_auto_desc)
                                        VpnDetectionMode.SYSTEM_ONLY -> stringResource(R.string.vpn_detection_mode_system_only_desc)
                                        VpnDetectionMode.NOTIFICATIONS_ONLY -> stringResource(R.string.vpn_detection_mode_notifications_only_desc)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            RadioButton(
                                selected = isSelected,
                                onClick = null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VpnProviderPriorityRow(
    rank: Int,
    item: VpnProviderItemInfo,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val moveUpLabel = stringResource(R.string.cd_move_up)
    val moveDownLabel = stringResource(R.string.cd_move_down)
    val appIconDesc = stringResource(R.string.cd_app_icon, item.name)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(32.dp)
            )

            AppIconLoader(packageName = item.packageName, contentDescription = appIconDesc)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onMoveUp,
                modifier = Modifier.size(32.dp),
                enabled = !isFirst
            ) {
                if (!isFirst) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = moveUpLabel,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            IconButton(
                onClick = onMoveDown,
                modifier = Modifier.size(32.dp),
                enabled = !isLast
            ) {
                if (!isLast) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = moveDownLabel,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            Switch(
                checked = item.enabled,
                onCheckedChange = onToggle
            )
        }
    }
}
