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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.models.NavContent
import com.d4viddf.hyperbridge.ui.components.NavDropdown
import com.d4viddf.hyperbridge.ui.components.NavPreview
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavCustomizationScreen(
    onBack: () -> Unit,
    packageName: String? = null,
    showTopBar: Boolean = true // <-- NEW FLAG
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

    Scaffold(
        topBar = {
            if (showTopBar) { // Only show if requested
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
                .verticalScroll(rememberScrollState()) // Allow scrolling to fit on smaller screens
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                    top = if (showTopBar) 16.dp else 0.dp // <-- DYNAMIC TOP PADDING
                )
        ) {

            Text("Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            NavPreview(currentLeft, currentRight)

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
                        .clickable {
                            if (isUsingGlobalDefault) {
                                scope.launch {
                                    preferences.updateAppNavLayout(
                                        packageName,
                                        globalLayout.first,
                                        globalLayout.second
                                    )
                                }
                            } else {
                                scope.launch {
                                    preferences.updateAppNavLayout(
                                        packageName,
                                        null,
                                        null
                                    )
                                }
                            }
                        }
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
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp)) {
                        NavDropdown(
                            label = stringResource(R.string.left_content),
                            selected = currentLeft,
                            onSelect = { newLeft ->
                                scope.launch {
                                    if (isGlobalMode) preferences.setGlobalNavLayout(newLeft, currentRight)
                                    else preferences.updateAppNavLayout(packageName, newLeft, currentRight)
                                }
                            }
                        )
                        Spacer(Modifier.height(16.dp))
                        NavDropdown(
                            label = stringResource(R.string.right_content),
                            selected = currentRight,
                            onSelect = { newRight ->
                                scope.launch {
                                    if (isGlobalMode) preferences.setGlobalNavLayout(currentLeft, newRight)
                                    else preferences.updateAppNavLayout(packageName, currentLeft, newRight)
                                }
                            }
                        )
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
}
