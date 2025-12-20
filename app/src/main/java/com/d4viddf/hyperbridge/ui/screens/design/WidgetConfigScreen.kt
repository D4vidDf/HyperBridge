package com.d4viddf.hyperbridge.ui.screens.design

import android.content.Intent
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.widget.WidgetManager
import com.d4viddf.hyperbridge.service.NotificationReaderService
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    widgetId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appPreferences = remember { AppPreferences(context.applicationContext) }

    // --- CONFIG STATES ---
    var isShowShade by remember { mutableStateOf(false) } // Default hidden from shade
    var timeoutSeconds by remember { mutableFloatStateOf(5f) } // Default 5s

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Widget") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- WIDGET PREVIEW ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        val wrapper = FrameLayout(ctx)
                        val hostView = WidgetManager.createPreview(ctx, widgetId)
                        if (hostView != null) {
                            // Critical for correct rendering
                            hostView.setAppWidget(widgetId, WidgetManager.getWidgetInfo(ctx, widgetId))
                            wrapper.addView(hostView)
                        }
                        wrapper
                    },
                    modifier = Modifier.padding(12.dp).fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SETTINGS CARD ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // Toggle: Show in Shade
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show in Notification Shade",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Keep notification in history",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isShowShade,
                            onCheckedChange = { isShowShade = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Slider: Timeout
                    Text(
                        text = "Duration: ${timeoutSeconds.roundToInt()} seconds",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = timeoutSeconds,
                        onValueChange = { timeoutSeconds = it },
                        valueRange = 2f..30f,
                        steps = 28
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- SAVE & LAUNCH ---
            Button(
                onClick = {
                    scope.launch {
                        // 1. Save Config
                        appPreferences.saveWidgetConfig(
                            id = widgetId,
                            isShowShade = isShowShade,
                            timeout = (timeoutSeconds * 1000).toLong()
                        )

                        // 2. Trigger Service
                        val intent = Intent(context, NotificationReaderService::class.java).apply {
                            action = "ACTION_TEST_WIDGET"
                            putExtra("WIDGET_ID", widgetId)
                        }
                        context.startService(intent)

                        // 3. Exit
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Save & Show on Island")
            }
        }
    }
}