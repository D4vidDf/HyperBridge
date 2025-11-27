package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LowPriority
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.models.IslandLimitMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrioritySettingsScreen(
    onBack: () -> Unit,
    onNavigateToPriorityList: () -> Unit // Navigation Callback
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    val currentMode by preferences.limitModeFlow.collectAsState(initial = IslandLimitMode.MOST_RECENT)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.island_behavior)) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            // 1. VISUALIZER ANIMATION AREA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                BehaviorVisualizer(currentMode)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. BIG TITLE
            Text(
                text = stringResource(R.string.limit_strategy),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.limit_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. OPTION CARDS
            IslandLimitMode.entries.forEach { mode ->
                BehaviorOptionCard(
                    mode = mode,
                    isSelected = currentMode == mode,
                    onClick = { scope.launch { preferences.setLimitMode(mode) } }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 4. PRIORITY CONFIG BUTTON (Only if Priority is selected)
            if (currentMode == IslandLimitMode.PRIORITY) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateToPriorityList,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.configure_order))
                }
            }
        }
    }
}

@Composable
fun BehaviorVisualizer(mode: IslandLimitMode) {
    val color = MaterialTheme.colorScheme.primary

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        // Visual representation based on mode
        when(mode) {
            IslandLimitMode.FIRST_COME -> {
                // 3 Dots, one crossed out
                Dot(color); Spacer(Modifier.width(4.dp)); Dot(color); Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error)
            }
            IslandLimitMode.MOST_RECENT -> {
                // Arrow replacing dot
                Dot(color.copy(0.3f)); Spacer(Modifier.width(4.dp)); Dot(color.copy(0.6f)); Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.History, null, tint = color)
            }
            IslandLimitMode.PRIORITY -> {
                // Stack hierarchy
                Icon(Icons.Default.LowPriority, null, tint = color, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun Dot(color: Color) {
    Box(modifier = Modifier.size(16.dp).background(color, CircleShape))
}

@Composable
fun BehaviorOptionCard(
    mode: IslandLimitMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "border"
    )
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    else
        MaterialTheme.colorScheme.surfaceContainerLow

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, borderColor) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isSelected, onClick = null) // Click handled by card
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(stringResource(mode.titleRes), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(mode.descRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}