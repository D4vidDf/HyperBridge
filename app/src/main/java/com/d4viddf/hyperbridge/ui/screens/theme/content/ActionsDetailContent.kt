package com.d4viddf.hyperbridge.ui.screens.theme.content

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.theme.ActionConfig
import com.d4viddf.hyperbridge.ui.components.EmptyState
import com.d4viddf.hyperbridge.ui.screens.theme.ShapeStyle
import com.d4viddf.hyperbridge.ui.screens.theme.ThemeViewModel
import com.d4viddf.hyperbridge.ui.screens.theme.getExpressiveShape

@Composable
fun ActionsDetailContent(viewModel: ThemeViewModel) {
    val actions = viewModel.themeDefaultActions
    var showSheet by remember { mutableStateOf(false) }
    var selectedKeyword by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val filteredActions = remember(actions, searchQuery) {
        if (searchQuery.isBlank()) {
            actions
        } else {
            actions.filterKeys { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedKeyword = null
                    showSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.theme_manager_cd_add))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(bottom = padding.calculateBottomPadding())
                .fillMaxSize()
        ) {

            // --- SEARCH BAR ---
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.actions_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null) } }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            }

            // --- LIST OR EMPTY STATE ---
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (actions.isEmpty() && searchQuery.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            title = stringResource(R.string.actions_empty_title),
                            description = stringResource(R.string.actions_empty_desc),
                            icon = Icons.Outlined.TouchApp
                        )
                    }
                } else if (filteredActions.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.actions_search_empty_fmt, searchQuery),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val actionList = filteredActions.entries.toList()
                        items(actionList.size) { index ->
                            val (keyword, config) = actionList[index]
                            val shape = getExpressiveShape(actionList.size, index, ShapeStyle.Large)

                            ActionItemCard(
                                keyword = keyword,
                                config = config,
                                shape = shape,
                                onClick = {
                                    selectedKeyword = keyword
                                    showSheet = true
                                },
                                onDelete = {
                                    viewModel.removeDefaultAction(keyword)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        ActionConfigSheet(
            initialKeyword = selectedKeyword,
            initialConfig = if (selectedKeyword != null) actions[selectedKeyword] else null,
            onDismiss = { showSheet = false },
            onSave = { keyword, config ->
                viewModel.updateDefaultAction(keyword, config)
                showSheet = false
            }
        )
    }
}

@Composable
fun ActionItemCard(
    keyword: String,
    config: ActionConfig,
    shape: Shape,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val bgColor = try { Color((config.backgroundColor ?: "#00000000").toColorInt()) } catch(_:Exception) { Color.Transparent }
    val tintColor = try { Color((config.tintColor ?: "#FFFFFF").toColorInt()) } catch(_:Exception) { Color.White }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = shape,
        modifier = Modifier.fillMaxWidth().height(88.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    color = tintColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = keyword,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = config.mode.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            ) {
                Icon(Icons.Rounded.Delete, null)
            }

            Spacer(Modifier.width(4.dp))

            Icon(
                Icons.AutoMirrored.Rounded.ArrowForwardIos,
                null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}