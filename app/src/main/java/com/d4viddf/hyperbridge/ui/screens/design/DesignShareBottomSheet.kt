package com.d4viddf.hyperbridge.ui.screens.design

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.translator.CompactPillConfig
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.PillLeftDesign
import com.d4viddf.hyperbridge.models.translator.PillRightDesign
import com.d4viddf.hyperbridge.models.translator.PresentationConfig
import com.d4viddf.hyperbridge.models.translator.ProgressSlotConfig
import com.d4viddf.hyperbridge.models.translator.ProgressSlotType
import com.d4viddf.hyperbridge.models.translator.TextSlotConfig
import com.d4viddf.hyperbridge.models.translator.TranslatorMetadata
import com.d4viddf.hyperbridge.ui.components.island.HyperOsIslandPreview
import com.d4viddf.hyperbridge.ui.components.island.IslandPreviewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignShareBottomSheet(
    design: CustomTranslator,
    title: String = stringResource(R.string.design_share_sheet_title),
    onDismiss: () -> Unit,
    onSharePackage: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Default to BOTH (Big + Pill)
    var previewMode by remember {
        mutableStateOf(IslandPreviewMode.BOTH)
    }

    var isSavingImage by remember { mutableStateOf(false) }

    val graphicsLayer = rememberGraphicsLayer()
    val pillGraphicsLayer = rememberGraphicsLayer()
    val expandedGraphicsLayer = rememberGraphicsLayer()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        DesignShareSheetContent(
            design = design,
            title = title,
            previewMode = previewMode,
            onPreviewModeChange = { previewMode = it },
            isSavingImage = isSavingImage,
            onSaveImages = {
                if (isSavingImage) return@DesignShareSheetContent
                isSavingImage = true
                scope.launch {
                    try {
                        val fileName = sanitizeFileName(design.meta.name.ifBlank { design.id })
                        if (previewMode == IslandPreviewMode.BOTH) {
                            val bannerBitmap = expandedGraphicsLayer.toImageBitmap().asAndroidBitmap()
                            val pillBitmap = pillGraphicsLayer.toImageBitmap().asAndroidBitmap()
                            val bannerSuccess = saveBitmapToPictures(context, bannerBitmap, "${fileName}_banner")
                            val pillSuccess = saveBitmapToPictures(context, pillBitmap, "${fileName}_pill")

                            if (bannerSuccess && pillSuccess) {
                                Toast.makeText(context, R.string.design_share_both_saved, Toast.LENGTH_SHORT).show()
                            } else if (bannerSuccess || pillSuccess) {
                                Toast.makeText(context, R.string.design_share_image_saved, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, R.string.design_share_image_failed, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val layer = when {
                                previewMode == IslandPreviewMode.COMPACT_PILL -> pillGraphicsLayer
                                previewMode == IslandPreviewMode.EXPANDED -> expandedGraphicsLayer
                                else -> graphicsLayer
                            }
                            val imageBitmap = layer.toImageBitmap()
                            val bitmap = imageBitmap.asAndroidBitmap()
                            val suffix = if (previewMode == IslandPreviewMode.COMPACT_PILL) "_pill" else "_banner"
                            val success = saveBitmapToPictures(context, bitmap, "$fileName$suffix")
                            if (success) {
                                val msgRes = if (previewMode == IslandPreviewMode.COMPACT_PILL) {
                                    R.string.design_share_pill_saved
                                } else {
                                    R.string.design_share_image_saved
                                }
                                Toast.makeText(context, msgRes, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, R.string.design_share_image_failed, Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (_: Exception) {
                        Toast.makeText(context, R.string.design_share_image_failed, Toast.LENGTH_SHORT).show()
                    } finally {
                        isSavingImage = false
                    }
                }
            },
            onShare = onSharePackage,
            graphicsLayerModifier = Modifier.drawWithContent {
                graphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(graphicsLayer)
            },
            statusBarModifier = Modifier.drawWithContent {
                pillGraphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(pillGraphicsLayer)
            },
            expandedModifier = Modifier.drawWithContent {
                expandedGraphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(expandedGraphicsLayer)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DesignShareSheetContent(
    design: CustomTranslator,
    title: String = stringResource(R.string.design_share_sheet_title),
    previewMode: IslandPreviewMode = IslandPreviewMode.BOTH,
    onPreviewModeChange: (IslandPreviewMode) -> Unit = {},
    isSavingImage: Boolean = false,
    onSaveImages: () -> Unit = {},
    onShare: () -> Unit = {},
    graphicsLayerModifier: Modifier = Modifier,
    statusBarModifier: Modifier = Modifier,
    expandedModifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: Title and Description
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = design.meta.name.ifBlank { stringResource(R.string.design_section_designs) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Segmented mode selector (Big + Pill, Big Island, Pill)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            val modes = listOf(
                IslandPreviewMode.BOTH to stringResource(R.string.design_share_mode_combined),
                IslandPreviewMode.EXPANDED to stringResource(R.string.design_share_mode_expanded),
                IslandPreviewMode.COMPACT_PILL to stringResource(R.string.design_share_mode_pill)
            )
            modes.forEachIndexed { index, (m, label) ->
                val isSelected = previewMode == m
                val shape = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = { onPreviewModeChange(m) },
                    shapes = shape,
                    colors = ToggleButtonDefaults.colors(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Preview Capture Container (captured to Bitmap via graphicsLayer)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF141414),
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(graphicsLayerModifier)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                HyperOsIslandPreview(
                    translator = design,
                    showChrome = false,
                    mode = previewMode,
                    statusBarModifier = statusBarModifier,
                    expandedModifier = expandedModifier
                )
            }
        }

        // Showcase Hint
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.design_share_showcase_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Actions: Save Images and Share (shares the package file)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val saveImagesLabel = if (previewMode == IslandPreviewMode.BOTH) {
                stringResource(R.string.design_share_btn_save_images)
            } else {
                stringResource(R.string.design_share_btn_save_image)
            }

            Button(
                onClick = onSaveImages,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                enabled = !isSavingImage
            ) {
                if (isSavingImage) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(
                        Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = saveImagesLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledTonalButton(
                onClick = onShare,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                enabled = !isSavingImage
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.design_share_btn_share),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

private fun sampleShareTranslator(): CustomTranslator = CustomTranslator(
    id = "sample-share-translator",
    meta = TranslatorMetadata(
        name = "Food Delivery Tracker",
        description = "Live food delivery updates with arrival timer",
        author = "HyperBridge",
        iconName = "delivery"
    ),
    presentation = PresentationConfig(
        textSlot = TextSlotConfig(
            titleTemplate = "Courier arriving soon",
            subtitleTemplate = "Toyota Camry • 2 km away",
            highlightTextTemplate = "Live Order"
        ),
        progressSlot = ProgressSlotConfig(
            type = ProgressSlotType.PROGRESS_BAR,
            showPercentage = true
        ),
        pill = CompactPillConfig(
            leftDesign = PillLeftDesign.ICON_AND_TEXT,
            rightDesign = PillRightDesign.TIMER
        )
    )
)

@Preview(name = "1. Design Share BottomSheet - Combined (Big + Pill)", showBackground = true, backgroundColor = 0xFF1C1B1F)
@Composable
private fun DesignShareBottomSheetCombinedPreview() {
    MaterialTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            DesignShareSheetContent(
                design = sampleShareTranslator(),
                previewMode = IslandPreviewMode.BOTH
            )
        }
    }
}

@Preview(name = "2. Design Share BottomSheet - Big Island Only", showBackground = true, backgroundColor = 0xFF1C1B1F)
@Composable
private fun DesignShareBottomSheetExpandedPreview() {
    MaterialTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            DesignShareSheetContent(
                design = sampleShareTranslator(),
                previewMode = IslandPreviewMode.EXPANDED
            )
        }
    }
}

@Preview(name = "3. Design Share BottomSheet - Compact Pill Only", showBackground = true, backgroundColor = 0xFF1C1B1F)
@Composable
private fun DesignShareBottomSheetPillPreview() {
    MaterialTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            DesignShareSheetContent(
                design = sampleShareTranslator(),
                previewMode = IslandPreviewMode.COMPACT_PILL
            )
        }
    }
}

private fun sanitizeFileName(name: String): String {
    return name.replace(Regex("[^a-zA-Z0-9._-]"), "_").trim('_').ifBlank { "hyperbridge_design" }
}

private suspend fun saveBitmapToPictures(context: Context, bitmap: Bitmap, fileName: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val uniqueName = "${fileName}_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, uniqueName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/HyperBridge")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (_: Exception) {
            false
        }
    }
}
