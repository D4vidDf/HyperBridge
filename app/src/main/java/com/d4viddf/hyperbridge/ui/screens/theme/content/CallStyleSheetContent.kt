package com.d4viddf.hyperbridge.ui.screens.theme.content

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.screens.theme.AssetPickerButton
import com.d4viddf.hyperbridge.ui.screens.theme.ThemeViewModel
import com.d4viddf.hyperbridge.ui.screens.theme.getShapeFromId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CallStyleSheetContent(viewModel: ThemeViewModel) {
    var tabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            HorizontalFloatingToolbar(
                expanded = true,
                content = {
                    Row(
                        modifier = Modifier.wrapContentWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ToolbarOption(
                            selected = tabIndex == 0,
                            icon = Icons.Rounded.Call,
                            text = stringResource(R.string.calls_label_answer),
                            onClick = { tabIndex = 0 }
                        )

                        ToolbarOption(
                            selected = tabIndex == 1,
                            icon = Icons.Rounded.CallEnd,
                            text = stringResource(R.string.calls_label_decline),
                            onClick = { tabIndex = 1 }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // [FIX] Removed top padding, only applied bottom padding for FAB
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                AnimatedContent(
                    targetState = tabIndex,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "CallTabTransition",
                    modifier = Modifier.padding(24.dp)
                ) { selectedTab ->
                    when (selectedTab) {
                        0 -> CallConfigTab(
                            title = stringResource(R.string.calls_label_answer),
                            color = viewModel.callAnswerColor,
                            selectedShapeId = viewModel.callAnswerShapeId,
                            onColorChange = { viewModel.callAnswerColor = it },
                            onShapeChange = { viewModel.callAnswerShapeId = it },
                            onAssetSelected = { uri ->
                                viewModel.stageAsset("call_answer", uri)
                                viewModel.callAnswerUri = uri
                            },
                            defaultIcon = Icons.Rounded.Call
                        )
                        1 -> CallConfigTab(
                            title = stringResource(R.string.calls_label_decline),
                            color = viewModel.callDeclineColor,
                            selectedShapeId = viewModel.callDeclineShapeId,
                            onColorChange = { viewModel.callDeclineColor = it },
                            onShapeChange = { viewModel.callDeclineShapeId = it },
                            onAssetSelected = { uri ->
                                viewModel.stageAsset("call_decline", uri)
                                viewModel.callDeclineUri = uri
                            },
                            defaultIcon = Icons.Rounded.CallEnd
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CallConfigTab(
    title: String,
    color: String,
    selectedShapeId: String,
    onColorChange: (String) -> Unit,
    onShapeChange: (String) -> Unit,
    onAssetSelected: (android.net.Uri) -> Unit,
    defaultIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AssetPickerButton("", defaultIcon) { uri -> onAssetSelected(uri) }
            }

            Spacer(Modifier.width(16.dp))

            OutlinedTextField(
                value = color,
                onValueChange = onColorChange,
                label = { Text("Hex Color") },
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(safeParseColor(color))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                },
                singleLine = true
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.icons_label_shape_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ThemeViewModel.ShapeOption.entries) { shapeOption ->
                    val isSelected = selectedShapeId == shapeOption.id
                    val shape = getShapeFromId(shapeOption.id).toShape()

                    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    val label = stringResource(shapeOption.labelRes)

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { onShapeChange(shapeOption.id) }
                            .semantics { contentDescription = label }
                            .background(containerColor, shape)
                            .border(2.dp, borderColor, shape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.Check,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}