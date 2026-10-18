package com.d4viddf.hyperbridge.ui.screens.design

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.core.content.FileProvider
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.translator.TranslatorRepository
import com.d4viddf.hyperbridge.models.translator.CompactPillConfig
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.PillLeftDesign
import com.d4viddf.hyperbridge.models.translator.PillRightDesign
import com.d4viddf.hyperbridge.models.translator.PresentationConfig
import com.d4viddf.hyperbridge.models.translator.PresentationMode
import com.d4viddf.hyperbridge.models.translator.ProgressSlotConfig
import com.d4viddf.hyperbridge.models.translator.ProgressSlotType
import com.d4viddf.hyperbridge.models.translator.TextSlotConfig
import com.d4viddf.hyperbridge.models.translator.TranslatorMetadata
import com.d4viddf.hyperbridge.ui.components.island.HyperOsIslandPreview
import com.d4viddf.hyperbridge.ui.components.island.IslandPreviewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

enum class ShareExportType {
    ALL,
    IMAGES,
    FILE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignShareBottomSheet(
    design: CustomTranslator,
    title: String = stringResource(R.string.design_share_sheet_title),
    isTranslator: Boolean = (design.presentation.mode != PresentationMode.WIDGET),
    onDismiss: () -> Unit,
    onSharePackage: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var exportType by remember { mutableStateOf(ShareExportType.ALL) }
    var isSaving by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }

    val graphicsLayer = rememberGraphicsLayer()
    val pillGraphicsLayer = rememberGraphicsLayer()
    val expandedGraphicsLayer = rememberGraphicsLayer()

    // Folder picker launcher for "Save opens folder selector to save"
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri == null) return@rememberLauncherForActivityResult
        if (isSaving) return@rememberLauncherForActivityResult
        isSaving = true
        scope.launch {
            try {
                val fileName = sanitizeFileName(design.meta.name.ifBlank { design.id })
                var savedCount = 0

                // 1. If ALL or FILE: save package file (.htrans)
                if (exportType == ShareExportType.ALL || exportType == ShareExportType.FILE) {
                    val packageUri = saveFileToTreeUri(
                        context = context,
                        treeUri = treeUri,
                        fileName = "$fileName.htrans",
                        mimeType = "application/octet-stream"
                    ) { outputStream ->
                        val repo = TranslatorRepository(context)
                        repo.exportTranslatorToZip(design, outputStream).getOrThrow()
                    }
                    if (packageUri != null) savedCount++
                }

                // 2. If ALL or IMAGES: save preview image(s)
                if (exportType == ShareExportType.ALL || exportType == ShareExportType.IMAGES) {
                    val bannerBitmap = expandedGraphicsLayer.toImageBitmap().asAndroidBitmap()
                    val bannerUri = saveFileToTreeUri(
                        context = context,
                        treeUri = treeUri,
                        fileName = "${fileName}_banner.png",
                        mimeType = "image/png"
                    ) { outputStream ->
                        bannerBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    if (bannerUri != null) savedCount++

                    if (isTranslator) {
                        val pillBitmap = pillGraphicsLayer.toImageBitmap().asAndroidBitmap()
                        val pillUri = saveFileToTreeUri(
                            context = context,
                            treeUri = treeUri,
                            fileName = "${fileName}_pill.png",
                            mimeType = "image/png"
                        ) { outputStream ->
                            pillBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        }
                        if (pillUri != null) savedCount++
                    }
                }

                if (savedCount > 0) {
                    Toast.makeText(context, R.string.design_share_saved_to_folder, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, R.string.design_share_save_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, R.string.design_share_save_failed, Toast.LENGTH_SHORT).show()
            } finally {
                isSaving = false
            }
        }
    }

    val handleShare = {
        if (!isSharing) {
            isSharing = true
            scope.launch {
                try {
                    val fileName = sanitizeFileName(design.meta.name.ifBlank { design.id })
                    when (exportType) {
                        ShareExportType.FILE -> {
                            onSharePackage()
                        }
                        ShareExportType.IMAGES -> {
                            val bannerBitmap = expandedGraphicsLayer.toImageBitmap().asAndroidBitmap()
                            val bannerUri = saveBitmapToCache(context, bannerBitmap, "${fileName}_banner")
                            val uris = ArrayList<Uri>()
                            if (bannerUri != null) uris.add(bannerUri)

                            if (isTranslator) {
                                val pillBitmap = pillGraphicsLayer.toImageBitmap().asAndroidBitmap()
                                val pillUri = saveBitmapToCache(context, pillBitmap, "${fileName}_pill")
                                if (pillUri != null) uris.add(pillUri)
                            }

                            if (uris.size == 1) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uris[0])
                                    putExtra(Intent.EXTRA_SUBJECT, design.meta.name)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                val chooser = Intent.createChooser(shareIntent, context.getString(R.string.design_share_btn_share)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(chooser)
                            } else if (uris.size > 1) {
                                val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "image/png"
                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                    putExtra(Intent.EXTRA_SUBJECT, design.meta.name)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                val chooser = Intent.createChooser(shareIntent, context.getString(R.string.design_share_btn_share)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(chooser)
                            } else {
                                Toast.makeText(context, R.string.design_share_image_failed, Toast.LENGTH_SHORT).show()
                            }
                        }
                        ShareExportType.ALL -> {
                            val repo = TranslatorRepository(context)
                            val packageFileResult = repo.exportTranslatorToFile(design)
                            val packageFile = packageFileResult.getOrNull()
                            val packageUri = packageFile?.let {
                                FileProvider.getUriForFile(context, "${context.packageName}.provider", it)
                            }

                            val bannerBitmap = expandedGraphicsLayer.toImageBitmap().asAndroidBitmap()
                            val bannerUri = saveBitmapToCache(context, bannerBitmap, "${fileName}_banner")

                            val uris = ArrayList<Uri>()
                            if (packageUri != null) uris.add(packageUri)
                            if (bannerUri != null) uris.add(bannerUri)

                            if (isTranslator) {
                                val pillBitmap = pillGraphicsLayer.toImageBitmap().asAndroidBitmap()
                                val pillUri = saveBitmapToCache(context, pillBitmap, "${fileName}_pill")
                                if (pillUri != null) uris.add(pillUri)
                            }

                            if (uris.isNotEmpty()) {
                                val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "*/*"
                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                    putExtra(Intent.EXTRA_SUBJECT, design.meta.name)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                val chooser = Intent.createChooser(shareIntent, context.getString(R.string.design_share_btn_share)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(chooser)
                            } else {
                                Toast.makeText(context, R.string.design_share_save_failed, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, R.string.design_share_save_failed, Toast.LENGTH_SHORT).show()
                } finally {
                    isSharing = false
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        DesignShareSheetContent(
            design = design,
            title = title,
            isTranslator = isTranslator,
            exportType = exportType,
            onExportTypeChange = { exportType = it },
            isSaving = isSaving,
            isSharing = isSharing,
            onSave = { folderPickerLauncher.launch(null) },
            onShare = handleShare,
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
    isTranslator: Boolean = (design.presentation.mode != PresentationMode.WIDGET),
    exportType: ShareExportType = ShareExportType.ALL,
    onExportTypeChange: (ShareExportType) -> Unit = {},
    isSaving: Boolean = false,
    isSharing: Boolean = false,
    onSave: () -> Unit = {},
    onShare: () -> Unit = {},
    graphicsLayerModifier: Modifier = Modifier,
    statusBarModifier: Modifier = Modifier,
    expandedModifier: Modifier = Modifier
) {
    // Preview stays same: Big + Pill for translators, Big for other. No more changing preview.
    val previewMode = if (isTranslator) IslandPreviewMode.BOTH else IslandPreviewMode.EXPANDED

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

        // Segmented export selector on top: All (files and images), Images, File
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            val types = listOf(
                ShareExportType.ALL to stringResource(R.string.design_share_export_all),
                ShareExportType.IMAGES to stringResource(R.string.design_share_export_images),
                ShareExportType.FILE to stringResource(R.string.design_share_export_file)
            )
            types.forEachIndexed { index, (type, label) ->
                val isSelected = exportType == type
                val shape = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    types.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = { onExportTypeChange(type) },
                    shapes = shape,
                    colors = ToggleButtonDefaults.colors(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
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
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)),
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

        // Actions: Save and Share
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                enabled = !isSaving && !isSharing
            ) {
                if (isSaving) {
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
                    text = stringResource(R.string.design_share_btn_save),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledTonalButton(
                onClick = onShare,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                enabled = !isSaving && !isSharing
            ) {
                if (isSharing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
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

@Preview(name = "1. Design Share BottomSheet - Translator (Big + Pill, All)", showBackground = true, backgroundColor = 0xFF1C1B1F)
@Composable
private fun DesignShareBottomSheetTranslatorPreview() {
    MaterialTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            DesignShareSheetContent(
                design = sampleShareTranslator(),
                isTranslator = true,
                exportType = ShareExportType.ALL
            )
        }
    }
}

@Preview(name = "2. Design Share BottomSheet - Custom Design (Big Only, Images)", showBackground = true, backgroundColor = 0xFF1C1B1F)
@Composable
private fun DesignShareBottomSheetDesignPreview() {
    MaterialTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            DesignShareSheetContent(
                design = sampleShareTranslator(),
                isTranslator = false,
                exportType = ShareExportType.IMAGES
            )
        }
    }
}

@Preview(name = "3. Design Share BottomSheet - File Only", showBackground = true, backgroundColor = 0xFF1C1B1F)
@Composable
private fun DesignShareBottomSheetFilePreview() {
    MaterialTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            DesignShareSheetContent(
                design = sampleShareTranslator(),
                isTranslator = true,
                exportType = ShareExportType.FILE
            )
        }
    }
}

private fun sanitizeFileName(name: String): String {
    return name.replace(Regex("[^a-zA-Z0-9._-]"), "_").trim('_').ifBlank { "hyperbridge_design" }
}

private suspend fun saveFileToTreeUri(
    context: Context,
    treeUri: Uri,
    fileName: String,
    mimeType: String,
    writeContent: suspend (OutputStream) -> Unit
): Uri? = withContext(Dispatchers.IO) {
    try {
        val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        val newFileUri = DocumentsContract.createDocument(
            context.contentResolver,
            parentDocUri,
            mimeType,
            fileName
        ) ?: return@withContext null

        context.contentResolver.openOutputStream(newFileUri)?.use { output ->
            writeContent(output)
        }
        newFileUri
    } catch (_: Exception) {
        null
    }
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
