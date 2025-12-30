package com.d4viddf.hyperbridge.ui.screens.theme.content

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
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
fun IconsDetailContent(viewModel: ThemeViewModel) {
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
                            icon = Icons.Outlined.FormatPaint,
                            text = stringResource(R.string.icons_tab_style),
                            onClick = { tabIndex = 0 }
                        )

                        ToolbarOption(
                            selected = tabIndex == 1,
                            icon = Icons.Outlined.Image,
                            text = stringResource(R.string.icons_tab_assets),
                            onClick = { tabIndex = 1 }
                        )

                        ToolbarOption(
                            selected = tabIndex == 2,
                            shapeIcon = MaterialShapes.Arch.toShape(),
                            text = stringResource(R.string.icons_tab_shape),
                            onClick = { tabIndex = 2 }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                    label = "IconsTabTransition",
                    modifier = Modifier.padding(24.dp)
                ) { selectedTab ->
                    when (selectedTab) {
                        0 -> IconsStyleTab(viewModel)
                        1 -> IconsAssetsTab(viewModel)
                        2 -> IconsShapeTab(viewModel)
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun ToolbarOption(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    shapeIcon: Shape? = null
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal

    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = contentColor, modifier = Modifier.size(20.dp))
            } else if (shapeIcon != null) {
                Box(modifier = Modifier.size(20.dp).background(contentColor, shapeIcon))
            }
            Spacer(Modifier.width(8.dp))
            Text(text, color = contentColor, style = MaterialTheme.typography.labelLarge, fontWeight = fontWeight)
        }
    }
}

@Composable
private fun IconsStyleTab(viewModel: ThemeViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            stringResource(R.string.icons_label_size),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Slider(
            value = viewModel.iconPaddingPercent.toFloat(),
            onValueChange = { viewModel.iconPaddingPercent = it.toInt() },
            valueRange = 0f..40f
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.icons_label_full), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(stringResource(R.string.icons_label_minimal), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun IconsAssetsTab(viewModel: ThemeViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(stringResource(R.string.icons_group_nav), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AssetPickerButton(stringResource(R.string.icons_btn_start), Icons.Rounded.Navigation) { uri -> viewModel.stageAsset("nav_start", uri) }
                AssetPickerButton(stringResource(R.string.icons_btn_end), Icons.Rounded.Flag) { uri -> viewModel.stageAsset("nav_end", uri) }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column {
            Text(stringResource(R.string.icons_group_progress), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                AssetPickerButton(stringResource(R.string.icons_btn_success), Icons.Rounded.CheckCircle) { uri -> viewModel.stageAsset("tick_icon", uri) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IconsShapeTab(viewModel: ThemeViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            stringResource(R.string.icons_label_shape_title),
            style = MaterialTheme.typography.titleMedium
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ThemeViewModel.ShapeOption.entries) { shapeOption ->
                val isSelected = viewModel.selectedShapeId == shapeOption.id
                val shape = getShapeFromId(shapeOption.id).toShape()

                val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

                // [UPDATED] Uses localized label from the enum
                val label = stringResource(shapeOption.labelRes)

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { viewModel.selectedShapeId = shapeOption.id }
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