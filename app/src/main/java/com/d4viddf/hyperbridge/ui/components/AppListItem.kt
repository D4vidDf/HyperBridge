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
            // Mark image as decorative so TalkBack ignores it (text already says name)
            Image(
                bitmap = app.icon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // SETTINGS BUTTON
                if (app.isBridged && onSettingsClick != null) {
                    IconButton(
                        onClick = onSettingsClick,
                        // Explicitly say "Configure WhatsApp"
                        modifier = Modifier.semantics { contentDescription = settingsLabel }
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null, // Description is on the button
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // TOGGLE SWITCH
                Switch(
                    checked = app.isBridged,
                    onCheckedChange = onToggle,

                    modifier = Modifier.semantics {
                        contentDescription = toggleLabel // "Toggle WhatsApp"
                        stateDescription = if (app.isBridged) activeState else inactiveState
                    }
                )
            }
        },
        // The whole row click also toggles, so we label the row too
        modifier = Modifier
            .clickable { onToggle(!app.isBridged) }
            .semantics { contentDescription = toggleLabel },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}