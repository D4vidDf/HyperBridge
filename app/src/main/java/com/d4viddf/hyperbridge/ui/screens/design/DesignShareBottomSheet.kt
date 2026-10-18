package com.d4viddf.hyperbridge.ui.screens.design

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.ui.components.island.HyperOsIslandPreview
import com.d4viddf.hyperbridge.ui.components.island.IslandPreviewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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

    var isExportingImage by remember { mutableStateOf(false) }
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title, Description, and Close Button
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
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.back))
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
                        onCheckedChange = { previewMode = m },
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
                        .drawWithContent {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HyperOsIslandPreview(
                        translator = design,
                        showChrome = false,
                        mode = previewMode,
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

            // Actions: Share Picture, Save Picture, Share Package
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val shareButtonLabel = when (previewMode) {
                    IslandPreviewMode.BOTH -> stringResource(R.string.design_share_btn_share_both)
                    IslandPreviewMode.EXPANDED -> stringResource(R.string.design_share_btn_share_expanded)
                    IslandPreviewMode.COMPACT_PILL -> stringResource(R.string.design_share_btn_share_pill)
                }

                val saveButtonLabel = when (previewMode) {
                    IslandPreviewMode.BOTH -> stringResource(R.string.design_share_btn_save_both)
                    IslandPreviewMode.EXPANDED -> stringResource(R.string.design_share_btn_save_expanded)
                    IslandPreviewMode.COMPACT_PILL -> stringResource(R.string.design_share_btn_save_pill)
                }

                Button(
                    onClick = {
                        if (isExportingImage) return@Button
                        isExportingImage = true
                        scope.launch {
                            try {
                                val fileName = sanitizeFileName(design.meta.name.ifBlank { design.id })
                                if (previewMode == IslandPreviewMode.BOTH) {
                                    val bannerBitmap = expandedGraphicsLayer.toImageBitmap().asAndroidBitmap()
                                    val pillBitmap = pillGraphicsLayer.toImageBitmap().asAndroidBitmap()
                                    val bannerUri = saveBitmapToCache(context, bannerBitmap, "${fileName}_banner")
                                    val pillUri = saveBitmapToCache(context, pillBitmap, "${fileName}_pill")

                                    val uris = ArrayList(listOfNotNull(bannerUri, pillUri))
                                    if (uris.isNotEmpty()) {
                                        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                            type = "image/png"
                                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                            putExtra(Intent.EXTRA_SUBJECT, design.meta.name)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        val chooser = Intent.createChooser(shareIntent, shareButtonLabel)
                                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(chooser)
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
                                    val uri = saveBitmapToCache(context, bitmap, "$fileName$suffix")
                                    if (uri != null) {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/png"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            putExtra(Intent.EXTRA_SUBJECT, design.meta.name)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        val chooser = Intent.createChooser(shareIntent, shareButtonLabel)
                                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(chooser)
                                    } else {
                                        Toast.makeText(context, R.string.design_share_image_failed, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, R.string.design_share_image_failed, Toast.LENGTH_SHORT).show()
                            } finally {
                                isExportingImage = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isExportingImage && !isSavingImage
                ) {
                    if (isExportingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(shareButtonLabel)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (isSavingImage) return@FilledTonalButton
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
                                } catch (e: Exception) {
                                    Toast.makeText(context, R.string.design_share_image_failed, Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSavingImage = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isExportingImage && !isSavingImage
                    ) {
                        if (isSavingImage) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(6.dp))
                        } else {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            saveButtonLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onSharePackage,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isExportingImage && !isSavingImage
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.design_share_btn_share_package),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun sanitizeFileName(name: String): String {
    return name.replace(Regex("[^a-zA-Z0-9._-]"), "_").trim('_').ifBlank { "hyperbridge_design" }
}

private suspend fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String): Uri? {
    return withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "previews").apply { mkdirs() }
            val file = File(cacheDir, "${fileName}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (_: Exception) {
            null
        }
    }
}

private suspend fun saveBitmapToPictures(context: Context, bitmap: Bitmap, fileName: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val uniqueName = "${fileName}_${System.currentTimeMillis()}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "HyperBridge").apply { mkdirs() }
                val file = File(appDir, uniqueName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
