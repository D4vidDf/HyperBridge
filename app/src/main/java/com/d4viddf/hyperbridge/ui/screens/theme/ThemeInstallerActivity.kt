package com.d4viddf.hyperbridge.ui.screens.theme

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

class ThemeInstallerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repo = ThemeRepository(applicationContext)
        val prefs = AppPreferences(applicationContext)
        val dataUri: Uri? = intent?.data

        setContent {
            // [FIX] Detect System Theme & Use Dynamic Colors
            val darkTheme = isSystemInDarkTheme()
            val context = LocalContext.current

            val colorScheme = when {
                darkTheme -> darkColorScheme()
                else -> lightColorScheme()
            }

            MaterialTheme(colorScheme = colorScheme) {
                InstallerScreen(
                    uri = dataUri,
                    repo = repo,
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
    repo: ThemeRepository,
    prefs: AppPreferences,
    onFinish: () -> Unit
) {
    var installState by remember { mutableStateOf<InstallState>(InstallState.Idle) }
    var installedTheme by remember { mutableStateOf<HyperTheme?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uri) {
        if (uri == null) {
            installState = InstallState.Error
            return@LaunchedEffect
        }

        installState = InstallState.Installing

        try {
            val themeId = repo.installThemeFromUri(uri)
            val themeFile = File(repo.getThemesDir(), "$themeId/theme_config.json")
            val json = Json { ignoreUnknownKeys = true }
            val theme = json.decodeFromString<HyperTheme>(themeFile.readText())

            installedTheme = theme
            installState = InstallState.Success(themeId)

        } catch (e: Exception) {
            e.printStackTrace()
            installState = InstallState.Error
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
            // Use surfaceContainerHigh for correct dialog contrast in M3
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
                    is InstallState.Success -> {
                        SuccessContent(
                            theme = installedTheme,
                            onApply = {
                                scope.launch {
                                    prefs.setActiveThemeId(state.themeId)
                                    repo.activateTheme(state.themeId)
                                    onFinish()
                                }
                            },
                            onCancel = onFinish
                        )
                    }
                    is InstallState.Error -> {
                        ErrorContent(onClose = onFinish)
                    }
                }
            }
        }
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
        text = stringResource(R.string.theme_installer_analyzing),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun SuccessContent(
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
        color = MaterialTheme.colorScheme.primary
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
private fun ErrorContent(onClose: () -> Unit) {
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
        text = stringResource(R.string.theme_installer_error_generic),
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
        Text(stringResource(R.string.theme_installer_btn_close))
    }
}

sealed class InstallState {
    data object Idle : InstallState()
    data object Installing : InstallState()
    data class Success(val themeId: String) : InstallState()
    data object Error : InstallState()
}

private val RequestIconShape = RoundedCornerShape(20.dp)

