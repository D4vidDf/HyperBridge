package com.d4viddf.hyperbridge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.IslandLimitMode

@Composable
fun PriorityEducationDialog(
    onDismiss: () -> Unit,
    onConfigure: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.priority_edu_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Show the animation for the DEFAULT mode (Most Recent)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    BehaviorVisualizer(mode = IslandLimitMode.MOST_RECENT)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // We manually parse bold tags since AlertDialog text doesn't support HTML natively in simple Text
                // For simplicity, we just show the raw string, or you can split it if you want styling.
                // Here we assume the string resource has text that looks good plain.
                Text(
                    text = stringResource(R.string.priority_edu_desc)
                        .replace("<b>", "").replace("</b>", ""), // Strip tags
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfigure) {
                Text(stringResource(R.string.configure_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.keep_default))
            }
        }
    )
}