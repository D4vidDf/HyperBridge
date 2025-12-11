package com.d4viddf.hyperbridge.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.AppInfo

@Composable
fun AppListItem(
    app: AppInfo,
    onToggle: (Boolean) -> Unit,
    onSettingsClick: (() -> Unit)? = null
) {
    // Strings for A11y
    val toggleLabel = stringResource(R.string.cd_toggle_app, app.name)
    val settingsLabel = stringResource(R.string.cd_configure_app, app.name)
    val activeState = stringResource(R.string.cd_app_state_active)
    val inactiveState = stringResource(R.string.cd_app_state_inactive)

    ListItem(
        headlineContent = {
            Text(
                text = app.name,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = app.packageName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.Gray
            )
        },
        leadingContent = {
            Image(
                bitmap = app.icon.asImageBitmap(),
                contentDescription = null, // Decorative, text provides name
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // RESTORED: Tune Icon (Only visible when app is active)
                // This gives the user a visual hint that settings are available.
                if (app.isBridged && onSettingsClick != null) {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.semantics { contentDescription = settingsLabel }
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Toggle Switch
                Switch(
                    checked = app.isBridged,
                    onCheckedChange = onToggle,
                    modifier = Modifier.semantics {
                        contentDescription = toggleLabel
                        stateDescription = if (app.isBridged) activeState else inactiveState
                    }
                )
            }
        },
        // ROW CLICK LOGIC:
        // If App is ON -> Open Settings
        // If App is OFF -> Turn ON (Toggle)
        modifier = Modifier
            .clickable {
                if (app.isBridged) {
                    onSettingsClick?.invoke()
                } else {
                    onToggle(true)
                }
            }
            .semantics { contentDescription = if (app.isBridged) settingsLabel else toggleLabel },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}