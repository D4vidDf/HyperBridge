package com.d4viddf.hyperbridge.ui.screens.theme

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.db.AppDatabase
import com.d4viddf.hyperbridge.data.model.HyperBridgeBackup
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.data.translator.TranslatorRepository
import com.d4viddf.hyperbridge.data.widget.CustomWidgetRepository
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.widget.CustomWidgetDocument
import com.d4viddf.hyperbridge.ui.screens.translators.getTranslatorOutlinedIcon
import com.d4viddf.hyperbridge.util.BackupManager
import com.d4viddf.hyperbridge.util.BackupSelection
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class ThemeInstallerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val themeRepo = ThemeRepository(applicationContext)
        val prefs = AppPreferences(applicationContext)
        val db = AppDatabase.getDatabase(applicationContext)
        val widgetRepo = CustomWidgetRepository(applicationContext)
        val translatorRepo = TranslatorRepository(applicationContext, db.translatorDao())
        val backupManager = BackupManager(applicationContext, prefs, db)
        val dataUri: Uri? = intent?.data

        setContent {
            val darkTheme = isSystemInDarkTheme()

            val colorScheme = when {
                darkTheme -> darkColorScheme()
                else -> lightColorScheme()
            }

            MaterialTheme(colorScheme = colorScheme) {
                InstallerScreen(
                    uri = dataUri,
                    themeRepo = themeRepo,
                    widgetRepo = widgetRepo,
                    translatorRepo = translatorRepo,
                    backupManager = backupManager,
                    prefs = prefs,
                    onFinish = { finish() }
                )
            }
        }
    }
}

@Composable
fun InstallerScreen(
    uri: Uri?,
    themeRepo: ThemeRepository,
    widgetRepo: CustomWidgetRepository,
    translatorRepo: TranslatorRepository,
    backupManager: BackupManager,
    prefs: AppPreferences,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var installState by remember { mutableStateOf<InstallState>(InstallState.Idle) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uri) {
        if (uri == null) {
            installState = InstallState.Error(context.getString(R.string.theme_installer_error_generic))
            return@LaunchedEffect
        }

        installState = InstallState.Installing

        try {
            when (val preview = inspectPackage(context, uri, themeRepo)) {
                is PackagePreview.Theme -> {
                    installState = InstallState.ThemeReady(preview.theme, preview.themeId)
                }
                is PackagePreview.CustomDesign -> {
                    installState = InstallState.WidgetReady(preview.widget, preview.translator, uri)
                }
                is PackagePreview.Translator -> {
                    installState = InstallState.TranslatorReady(preview.translator, uri)
                }
                is PackagePreview.Backup -> {
                    installState = InstallState.BackupReady(preview.backup, uri)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            installState = InstallState.Error(e.localizedMessage ?: context.getString(R.string.theme_installer_error_generic))
        }
    }

    Dialog(
        onDismissRequest = onFinish,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.width(320.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val state = installState) {
                    is InstallState.Idle, is InstallState.Installing -> {
                        LoadingContent()
                    }
                    is InstallState.ThemeReady -> {
                        ThemeSuccessContent(
                            theme = state.theme,
                            onApply = {
                                scope.launch {
                                    prefs.setActiveThemeId(state.themeId)
                                    themeRepo.activateTheme(state.themeId)
                                    onFinish()
                                }
                            },
                            onCancel = onFinish
                        )
                    }
                    is InstallState.WidgetReady -> {
                        WidgetReadyContent(
                            widgetDoc = state.widgetDoc,
                            translator = state.translator,
                            onImport = {
                                scope.launch {
                                    try {
                                        widgetRepo.installWidgetFromUri(state.uri)
                                        installState = InstallState.SuccessMessage(
                                            title = context.getString(R.string.widget_installer_success_title),
                                            message = context.getString(
                                                R.string.widget_installer_success_message,
                                                state.widgetDoc.meta.name.ifBlank { state.widgetDoc.id }
                                            )
                                        )
                                    } catch (e: Exception) {
                                        installState = InstallState.Error(
                                            e.localizedMessage ?: context.getString(R.string.theme_installer_error_generic)
                                        )
                                    }
                                }
                            },
                            onCancel = onFinish
                        )
                    }
                    is InstallState.TranslatorReady -> {
                        TranslatorReadyContent(
                            translator = state.translator,
                            onImport = {
                                scope.launch {
                                    try {
                                        val stream = context.contentResolver.openInputStream(state.uri)
                                            ?: throw IllegalArgumentException("Could not open input file")
                                        stream.use { s ->
                                            translatorRepo.importTranslatorFromStream(s).getOrThrow()
                                        }
                                        installState = InstallState.SuccessMessage(
                                            title = context.getString(R.string.translator_installer_success_title),
                                            message = context.getString(
                                                R.string.translator_installer_success_message,
                                                state.translator.meta.name.ifBlank { state.translator.id }
                                            )
                                        )
                                    } catch (e: Exception) {
                                        installState = InstallState.Error(
                                            e.localizedMessage ?: context.getString(R.string.theme_installer_error_generic)
                                        )
                                    }
                                }
                            },
                            onCancel = onFinish
                        )
                    }
                    is InstallState.BackupReady -> {
                        BackupReadyContent(
                            backup = state.backup,
                            onRestore = {
                                scope.launch {
                                    try {
                                        backupManager.restoreBackup(state.backup, BackupSelection()).getOrThrow()
                                        installState = InstallState.SuccessMessage(
                                            title = context.getString(R.string.backup_installer_success_title),
                                            message = context.getString(R.string.backup_installer_success_message)
                                        )
                                    } catch (e: Exception) {
                                        installState = InstallState.Error(
                                            e.localizedMessage ?: context.getString(R.string.theme_installer_error_generic)
                                        )
                                    }
                                }
                            },
                            onCancel = onFinish
                        )
                    }
                    is InstallState.SuccessMessage -> {
                        SuccessMessageContent(
                            title = state.title,
                            message = state.message,
                            onDone = onFinish
                        )
                    }
                    is InstallState.Error -> {
                        ErrorContent(
                            message = state.message,
                            onClose = onFinish
                        )
                    }
                }
            }
        }
    }
}

// --- PACKAGE INSPECTION ---

private sealed class PackagePreview {
    data class Theme(val theme: HyperTheme, val themeId: String) : PackagePreview()
    data class CustomDesign(val widget: CustomWidgetDocument, val translator: CustomTranslator?) : PackagePreview()
    data class Translator(val translator: CustomTranslator) : PackagePreview()
    data class Backup(val backup: HyperBridgeBackup) : PackagePreview()
}

private fun readEntryBytes(zip: ZipInputStream): ByteArray {
    val baos = ByteArrayOutputStream()
    val buf = ByteArray(4096)
    var r: Int
    while (zip.read(buf).also { r = it } != -1) {
        baos.write(buf, 0, r)
    }
    return baos.toByteArray()
}

private suspend fun inspectPackage(
    context: Context,
    uri: Uri,
    themeRepo: ThemeRepository
): PackagePreview = withContext(Dispatchers.IO) {
    val isZip = context.contentResolver.openInputStream(uri)?.use { stream ->
        val header = ByteArray(4)
        val read = stream.read(header)
        read == 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
                header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
    } ?: false

    val json = Json { ignoreUnknownKeys = true }

    if (isZip) {
        var themeConfigText: String? = null
        var widgetConfigText: String? = null
        var translatorConfigText: String? = null

        context.contentResolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val simpleName = entry.name.replace('\\', '/').substringAfterLast('/')
                    when (simpleName) {
                        "theme_config.json" -> {
                            themeConfigText = readEntryBytes(zip).toString(Charsets.UTF_8)
                        }
                        "widget.json" -> {
                            widgetConfigText = readEntryBytes(zip).toString(Charsets.UTF_8)
                        }
                        "translator.json" -> {
                            translatorConfigText = readEntryBytes(zip).toString(Charsets.UTF_8)
                        }
                    }
                    entry = zip.nextEntry
                }
            }
        }

        when {
            themeConfigText != null -> {
                val theme = json.decodeFromString<HyperTheme>(themeConfigText)
                val themeId = themeRepo.installThemeFromUri(uri)
                PackagePreview.Theme(theme, themeId)
            }
            widgetConfigText != null -> {
                val widgetDoc = json.decodeFromString<CustomWidgetDocument>(widgetConfigText)
                val translator = translatorConfigText?.let {
                    CustomTranslator.fromJson(it).getOrNull()
                }
                PackagePreview.CustomDesign(widgetDoc, translator)
            }
            translatorConfigText != null -> {
                val translator = CustomTranslator.fromJson(translatorConfigText).getOrThrow()
                PackagePreview.Translator(translator)
            }
            else -> throw IllegalArgumentException(context.getString(R.string.package_installer_unsupported))
        }
    } else {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IllegalArgumentException("Could not read file")

        // 1. Try parsing as Backup
        try {
            val backup = Gson().fromJson(text, HyperBridgeBackup::class.java)
            if (backup?.metadata?.packageName == context.packageName) {
                return@withContext PackagePreview.Backup(backup)
            }
        } catch (_: Exception) {}

        // 2. Try parsing as Translator
        val parsedTranslator = CustomTranslator.fromJson(text).getOrNull()
        if (parsedTranslator != null) {
            return@withContext PackagePreview.Translator(parsedTranslator)
        }

        // 3. Try parsing as Widget Document
        try {
            val doc = json.decodeFromString<CustomWidgetDocument>(text)
            if (doc.id.isNotBlank()) {
                return@withContext PackagePreview.CustomDesign(doc, null)
            }
        } catch (_: Exception) {}

        throw IllegalArgumentException(context.getString(R.string.package_installer_unsupported))
    }
}

// --- SUB-COMPONENTS ---

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RequestIconShape),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.package_installer_analyzing),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun ThemeSuccessContent(
    theme: HyperTheme?,
    onApply: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(MaterialTheme.colorScheme.tertiaryContainer, RequestIconShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Palette,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(32.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.theme_installer_success_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = theme?.meta?.name ?: "Unknown Theme",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.theme_installer_author, theme?.meta?.author ?: "Unknown"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = " • ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.theme_installer_version, theme?.meta?.version ?: 1),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = stringResource(R.string.theme_installer_apply_question),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(24.dp))

    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.theme_installer_action_apply))
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.theme_installer_action_later))
        }
    }
}

@Composable
private fun WidgetReadyContent(
    widgetDoc: CustomWidgetDocument,
    translator: CustomTranslator?,
    onImport: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RequestIconShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Widgets,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(32.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.widget_installer_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = widgetDoc.meta.name.ifBlank { widgetDoc.id },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.widget_installer_dimensions, 350, widgetDoc.canvas.heightDp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (translator != null) {
            Text(
                text = " • ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.widget_installer_linked_translator, translator.meta.name),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = stringResource(R.string.widget_installer_apply_question),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(24.dp))

    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onImport,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.widget_installer_action_import))
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.package_installer_action_cancel))
        }
    }
}

@Composable
private fun TranslatorReadyContent(
    translator: CustomTranslator,
    onImport: () -> Unit,
    onCancel: () -> Unit
) {
    val iconVector: ImageVector = getTranslatorOutlinedIcon(translator.meta.iconName)

    Box(
        modifier = Modifier
            .size(72.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, RequestIconShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(32.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.translator_installer_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = translator.meta.name.ifBlank { translator.id },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

    Text(
        text = translator.meta.author.ifBlank { translator.presentation.mode.name },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = stringResource(R.string.translator_installer_apply_question),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(24.dp))

    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onImport,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.translator_installer_action_import))
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.package_installer_action_cancel))
        }
    }
}

@Composable
private fun BackupReadyContent(
    backup: HyperBridgeBackup,
    onRestore: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(MaterialTheme.colorScheme.tertiaryContainer, RequestIconShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Restore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(32.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.backup_installer_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = backup.metadata.deviceModel,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

    Text(
        text = "${backup.settings.size} items • v${backup.metadata.versionName}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = stringResource(R.string.backup_installer_apply_question),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(24.dp))

    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onRestore,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.backup_installer_action_restore))
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.package_installer_action_cancel))
        }
    }
}

@Composable
private fun SuccessMessageContent(
    title: String,
    message: String,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RequestIconShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(32.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onDone,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(stringResource(R.string.package_installer_action_done))
    }
}

@Composable
private fun ErrorContent(
    message: String?,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(MaterialTheme.colorScheme.errorContainer, RequestIconShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(32.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.theme_installer_error_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = message ?: stringResource(R.string.theme_installer_error_generic),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onClose,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        )
    ) {
        Text(stringResource(R.string.close))
    }
}

sealed class InstallState {
    data object Idle : InstallState()
    data object Installing : InstallState()
    data class ThemeReady(val theme: HyperTheme?, val themeId: String) : InstallState()
    data class WidgetReady(
        val widgetDoc: CustomWidgetDocument,
        val translator: CustomTranslator?,
        val uri: Uri
    ) : InstallState()
    data class TranslatorReady(
        val translator: CustomTranslator,
        val uri: Uri
    ) : InstallState()
    data class BackupReady(
        val backup: HyperBridgeBackup,
        val uri: Uri
    ) : InstallState()
    data class SuccessMessage(
        val title: String,
        val message: String
    ) : InstallState()
    data class Error(val message: String? = null) : InstallState()
}

private val RequestIconShape = RoundedCornerShape(20.dp)
