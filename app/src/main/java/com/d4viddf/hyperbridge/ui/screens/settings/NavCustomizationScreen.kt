package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.models.NavContent
import com.d4viddf.hyperbridge.ui.components.NavPreview
import com.d4viddf.hyperbridge.ui.components.getNavContentLabelRes
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme
import kotlinx.coroutines.launch

@Composable
fun NavCustomizationScreen(
    onBack: () -> Unit,
    packageName: String? = null,
    showTopBar: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    // 1. Get Global Fallback
    val globalLayout by preferences.globalNavLayoutFlow.collectAsState(initial = NavContent.DISTANCE_ETA to NavContent.INSTRUCTION)

    // 2. Get local AppPreference (if packageName is provided)
    val appLayout by if (packageName != null) {
        preferences.getAppNavLayout(packageName).collectAsState(initial = null to null)
    } else {
        remember { mutableStateOf<Pair<NavContent?, NavContent?>>(null to null) }
    }

    // 3. Resolve what is currently active
    val isGlobalMode = packageName == null
    val isUsingGlobalDefault = !isGlobalMode && appLayout.first == null
    val currentLeft = appLayout.first ?: globalLayout.first
    val currentRight = appLayout.second ?: globalLayout.second

    NavCustomizationContent(
        leftContent = currentLeft,
        rightContent = currentRight,
        isGlobalMode = isGlobalMode,
        isUsingGlobalDefault = isUsingGlobalDefault,
        showTopBar = showTopBar,
        onBack = onBack,
        onToggleUseGlobalDefault = {
            if (packageName != null) {
                scope.launch {
                    if (isUsingGlobalDefault) {
                        preferences.updateAppNavLayout(packageName, globalLayout.first, globalLayout.second)
                    } else {
                        preferences.updateAppNavLayout(packageName, null, null)
                    }
                }
            }
        },
        onLeftContentChange = { newLeft ->
            scope.launch {
                if (packageName == null) {
                    preferences.setGlobalNavLayout(newLeft, currentRight)
                } else {
                    preferences.updateAppNavLayout(packageName, newLeft, currentRight)
                }
            }
        },
        onRightContentChange = { newRight ->
            scope.launch {
                if (packageName == null) {
                    preferences.setGlobalNavLayout(currentLeft, newRight)
                } else {
                    preferences.updateAppNavLayout(packageName, currentLeft, newRight)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavCustomizationContent(
    leftContent: NavContent,
    rightContent: NavContent,
    isGlobalMode: Boolean,
    isUsingGlobalDefault: Boolean,
    showTopBar: Boolean = true,
    onBack: () -> Unit,
    onToggleUseGlobalDefault: () -> Unit,
    onLeftContentChange: (NavContent) -> Unit,
    onRightContentChange: (NavContent) -> Unit
) {
    var showLeftSheet by remember { mutableStateOf(false) }
    var showRightSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text(stringResource(R.string.nav_layout_title)) },
                    navigationIcon = {
                        FilledTonalIconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                    top = if (showTopBar) 16.dp else 0.dp
                )
        ) {
            Text(
                stringResource(R.string.preview),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            NavPreview(leftContent, rightContent)

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                stringResource(R.string.group_configuration),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!isGlobalMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleUseGlobalDefault() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isUsingGlobalDefault, onCheckedChange = null)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.use_global_default), style = MaterialTheme.typography.bodyLarge)
                }
            }

            val controlsEnabled = isGlobalMode || !isUsingGlobalDefault
            if (controlsEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        // Left Content Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLeftSheet = true }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.left_content),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = stringResource(getNavContentLabelRes(leftContent)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // Right Content Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showRightSheet = true }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.right_content),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = stringResource(getNavContentLabelRes(rightContent)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // --- Informational Notes ---
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.good_to_know),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.nav_layout_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Left Content Bottom Sheet
    if (showLeftSheet) {
        NavOptionBottomSheet(
            title = stringResource(R.string.left_content),
            options = NavContent.entries,
            selected = leftContent,
            labelFor = { option -> stringResource(getNavContentLabelRes(option)) },
            onSelect = { newLeft ->
                onLeftContentChange(newLeft)
            },
            onDismiss = { showLeftSheet = false }
        )
    }

    // Right Content Bottom Sheet
    if (showRightSheet) {
        NavOptionBottomSheet(
            title = stringResource(R.string.right_content),
            options = NavContent.entries,
            selected = rightContent,
            labelFor = { option -> stringResource(getNavContentLabelRes(option)) },
            onSelect = { newRight ->
                onRightContentChange(newRight)
            },
            onDismiss = { showRightSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> NavOptionBottomSheet(
    title: String,
    options: List<T>,
    selected: T,
    labelFor: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            options.forEach { option ->
                val isSelected = option == selected
                Surface(
                    onClick = {
                        onSelect(option)
                        onDismiss()
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
                        Text(
                            text = labelFor(option),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
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

@Preview(showBackground = true)
@Composable
fun NavCustomizationScreenPreview() {
    HyperBridgeTheme {
        NavCustomizationContent(
            leftContent = NavContent.DISTANCE_ETA,
            rightContent = NavContent.INSTRUCTION,
            isGlobalMode = true,
            isUsingGlobalDefault = false,
            showTopBar = true,
            onBack = {},
            onToggleUseGlobalDefault = {},
            onLeftContentChange = {},
            onRightContentChange = {}
        )
    }
}

