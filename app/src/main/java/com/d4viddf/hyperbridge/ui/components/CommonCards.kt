package com.d4viddf.hyperbridge.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.util.openAutoStartSettings // Correct Import
import com.d4viddf.hyperbridge.util.openBatterySettings   // Correct Import

// ... (Keep WarningCard and ExpandableOptimizationCard code from previous step) ...
@Composable
fun WarningCard(
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, null)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun ExpandableOptimizationCard(context: Context) {
    var expanded by remember { mutableStateOf(false) }
    val cardColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, null, tint = contentColor)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Xiaomi System Setup", fontWeight = FontWeight.Bold, color = contentColor)
                Spacer(modifier = Modifier.weight(1f))
                Text(if (expanded) "Hide" else "Show", color = contentColor)
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "HyperOS kills background apps. Apply these settings:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { openAutoStartSettings(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = contentColor)
                ) {
                    Text("1. Enable Autostart", color = MaterialTheme.colorScheme.surface)
                }

                OutlinedButton(
                    onClick = { openBatterySettings(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
                ) {
                    Text("2. Set Battery 'No Restrictions'")
                }
            }
        }
    }
}

@Composable
fun ListOptionCard(title: String, subtitle: String, icon: ImageVector, shape: Shape, onClick: () -> Unit, trailingContent: (@Composable () -> Unit)? = null) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = shape, modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp)) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailingContent != null) trailingContent() else Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}