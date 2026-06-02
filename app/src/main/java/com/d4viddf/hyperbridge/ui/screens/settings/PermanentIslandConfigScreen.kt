package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme
import com.d4viddf.hyperbridge.ui.components.PermanentIslandPreview

@Composable
fun PermanentIslandConfigScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = AppPreferences(context)
    val scope = rememberCoroutineScope()
    
    val isEnabled by prefs.isPermanentIslandEnabledFlow.collectAsState(initial = false)
    val islandWidth by prefs.permanentIslandWidthFlow.collectAsState(initial = 0)

    PermanentIslandConfigContent(
        isEnabled = isEnabled,
        islandWidth = islandWidth,
        onEnabledChange = { checked ->
            scope.launch {
                prefs.setPermanentIslandEnabled(checked)
            }
        },
        onWidthChange = { width ->
            scope.launch {
                prefs.setPermanentIslandWidth(width)
            }
        },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermanentIslandConfigContent(
    isEnabled: Boolean,
    islandWidth: Int,
    onEnabledChange: (Boolean) -> Unit,
    onWidthChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    var sliderValue by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var isDragging by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(islandWidth) {
        if (!isDragging) {
            sliderValue = islandWidth.toFloat()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.permanent_island_title)) },
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
                .padding(16.dp)
                .fillMaxSize()
        ) {
            PermanentIslandPreview(islandWidthValue = if (isDragging) sliderValue.toInt() else islandWidth)

            Spacer(Modifier.height(24.dp))
            


            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.permanent_island_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )

                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = onEnabledChange
                        )
                    }
                    
                    if (isEnabled) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.permanent_island_width),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                        Slider(
                            value = sliderValue,
                            onValueChange = { value ->
                                isDragging = true
                                sliderValue = value
                                onWidthChange(value.toInt())
                            },
                            onValueChangeFinished = {
                                isDragging = false
                            },
                            valueRange = 0f..20f
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.permanent_island_screen_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

        }
    }
}

@Preview(showBackground = true)
@Composable
fun PermanentIslandConfigScreenPreview() {
    HyperBridgeTheme {
        PermanentIslandConfigContent(
            isEnabled = true,
            islandWidth = 10,
            onEnabledChange = {},
            onWidthChange = {},
            onBack = {}
        )
    }
}
