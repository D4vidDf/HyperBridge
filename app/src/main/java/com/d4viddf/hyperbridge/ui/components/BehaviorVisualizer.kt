package com.d4viddf.hyperbridge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LowPriority
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.models.IslandLimitMode

@Composable
fun BehaviorVisualizer(mode: IslandLimitMode) {
    val color = MaterialTheme.colorScheme.primary

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        when(mode) {
            IslandLimitMode.FIRST_COME -> {
                Dot(color); Spacer(Modifier.width(4.dp))
                Dot(color); Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error)
            }
            IslandLimitMode.MOST_RECENT -> {
                Dot(color.copy(0.3f)); Spacer(Modifier.width(4.dp))
                Dot(color.copy(0.6f)); Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.History, null, tint = color)
            }
            IslandLimitMode.PRIORITY -> {
                Icon(Icons.Default.LowPriority, null, tint = color, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(modifier = Modifier.size(16.dp).background(color, CircleShape))
}