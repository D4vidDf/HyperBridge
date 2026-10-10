package com.d4viddf.hyperbridge.ui.screens.design.studio

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class StudioAppItem(
    val name: String,
    val packageName: String,
    val icon: Bitmap? = null
)

private val MOCK_STUDIO_APPS = listOf(
    StudioAppItem("Spotify", "com.spotify.music"),
    StudioAppItem("Telegram", "org.telegram.messenger"),
    StudioAppItem("WhatsApp", "com.whatsapp"),
    StudioAppItem("Google Calendar", "com.google.android.calendar"),
    StudioAppItem("Settings", "com.android.settings"),
    StudioAppItem("Phone", "com.google.android.dialer"),
    StudioAppItem("Clock", "com.google.android.deskclock"),
    StudioAppItem("Maps", "com.google.android.apps.maps")
)

/**
 * Dialog allowing the user to select an installed application for OpenApp actions or AppIcon sources.
 */
@Composable
fun StudioAppChooserDialog(
    onDismiss: () -> Unit,
    onAppSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    var searchQuery by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<StudioAppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(!isPreview) }

    LaunchedEffect(isPreview) {
        if (isPreview) {
            apps = MOCK_STUDIO_APPS
            isLoading = false
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val pm = context.packageManager
                    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                    val resolveInfos = pm.queryIntentActivities(intent, 0)
                    val loaded = resolveInfos.map { ri ->
                        val name = ri.loadLabel(pm).toString()
                        val pkg = ri.activityInfo.packageName
                        val iconDrawable = ri.loadIcon(pm)
                        val iconBmp = (iconDrawable as? BitmapDrawable)?.bitmap
                        StudioAppItem(name = name, packageName = pkg, icon = iconBmp)
                    }.sortedBy { it.name.lowercase() }

                    withContext(Dispatchers.Main) {
                        apps = if (loaded.isNotEmpty()) loaded else MOCK_STUDIO_APPS
                        isLoading = false
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        apps = MOCK_STUDIO_APPS
                        isLoading = false
                    }
                }
            }
        }
    }

    val filteredApps = remember(searchQuery, apps) {
        if (searchQuery.isBlank()) apps
        else {
            val q = searchQuery.trim().lowercase()
            apps.filter { it.name.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.studio_app_chooser_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.studio_app_chooser_search)) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                } else if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.studio_app_chooser_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAppSelected(app.packageName) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (app.icon != null) {
                                        Image(
                                            bitmap = app.icon.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Apps,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Spacer(Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = app.packageName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
