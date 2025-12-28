package com.d4viddf.hyperbridge.ui.screens.theme

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import androidx.core.graphics.toColorInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeManagerScreen(
    onBack: () -> Unit,
    onFindThemes: () -> Unit,
    onCreateTheme: () -> Unit,
    onEditTheme: (String) -> Unit // [NEW] Callback to edit specific ID
) {
    val viewModel: ThemeViewModel = viewModel()
    val installedThemes by viewModel.installedThemes.collectAsState()
    val activeId by viewModel.activeThemeId.collectAsState()

    var showAddOptions by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Ensure list is fresh when screen opens
    LaunchedEffect(Unit) {
        viewModel.refreshThemes()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theme_manager_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.theme_manager_cd_back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddOptions = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.theme_manager_cd_add)
                )
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Default Option
            item {
                SystemDefaultCard(
                    isActive = activeId == null,
                    onClick = { viewModel.resetToDefault() }
                )
            }

            // 2. Custom Themes
            items(installedThemes) { theme ->
                ThemeCard(
                    theme = theme,
                    isActive = activeId == theme.id,
                    onClick = { viewModel.applyTheme(theme) },
                    onDelete = { viewModel.deleteTheme(theme) },
                    onExport = { viewModel.exportAndShareTheme(theme) },
                    onEdit = { onEditTheme(theme.id) } // [NEW] Pass ID to edit
                )
            }
        }
    }

    if (showAddOptions) {
        ModalBottomSheet(
            onDismissRequest = { showAddOptions = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 48.dp)
            ) {
                Text(
                    text = stringResource(R.string.theme_sheet_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Button(
                    onClick = {
                        showAddOptions = false
                        onCreateTheme()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Brush, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.theme_sheet_action_create), style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        showAddOptions = false
                        onFindThemes()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.theme_sheet_action_find), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun ThemeCard(
    theme: HyperTheme,
    isActive: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onEdit: () -> Unit // [NEW] Parameter
) {
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isActive) 2.dp else 0.dp

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(borderWidth, borderColor),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(parseColor(theme.global.highlightColor))
                )
                Spacer(modifier = Modifier.weight(1f))
                if (isActive) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = stringResource(R.string.theme_card_cd_active),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info
            Text(
                text = theme.meta.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = stringResource(R.string.theme_card_author_format, theme.meta.author),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Actions Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                // Edit Button
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Export Button
                IconButton(onClick = onExport, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.Share,
                        contentDescription = "Export",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Delete Button
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.theme_card_cd_delete),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SystemDefaultCard(
    isActive: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isActive) 2.dp else 0.dp

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(borderWidth, borderColor),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PhoneAndroid, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                if (isActive) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.theme_system_default_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.theme_system_default_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(44.dp))
        }
    }
}

fun parseColor(hex: String?): Color {
    return try {
        if (hex.isNullOrEmpty()) Color.Black else Color(hex.toColorInt())
    } catch (e: Exception) { Color.Black }
}