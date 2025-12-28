package com.d4viddf.hyperbridge.ui.screens.theme

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

        // Manual Dependency Injection (Replace with Hilt if you use it)
        val repo = ThemeRepository(applicationContext)
        val prefs = AppPreferences(applicationContext)

        // Get the file URI from the intent
        val dataUri: Uri? = intent?.data

        setContent {
            // We use a transparent background for the activity,
            // and show a Dialog composable on top.
            MaterialTheme {
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

    // Auto-start installation when the dialog opens
    LaunchedEffect(uri) {
        if (uri == null) {
            installState = InstallState.Error
            return@LaunchedEffect
        }

        installState = InstallState.Installing

        try {
            // 1. Install the theme to internal storage
            val themeId = repo.installThemeFromUri(uri)

            // 2. Read the metadata back to show the user
            // (We construct a temporary file reader just to get the name/author)
            // Ideally, repo.installThemeFromUri should return the Theme object, but this works:
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

    // The Main Dialog
    Dialog(
        onDismissRequest = onFinish,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false // Force user to choose
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
                    is InstallState.Success -> {
                        SuccessContent(
                            theme = installedTheme,
                            onApply = {
                                scope.launch {
                                    prefs.setActiveThemeId(state.themeId)
                                    // Also notify repository to refresh memory
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
    // Icon Header
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

    // Title
    Text(
        text = stringResource(R.string.theme_installer_success_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Theme Name
    Text(
        text = theme?.meta?.name ?: "Unknown Theme",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )

    // Author & Version
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

    // Question
    Text(
        text = stringResource(R.string.theme_installer_apply_question),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Actions
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

// Simple internal state for the UI logic
sealed class InstallState {
    data object Idle : InstallState()
    data object Installing : InstallState()
    data class Success(val themeId: String) : InstallState()
    data object Error : InstallState()
}

// M3 Expressive Shape for the Icons
private val RequestIconShape = RoundedCornerShape(20.dp)

// Helper to access internal Dir from Repo if needed (or add a getter to ThemeRepository)
private fun ThemeRepository.getThemesDir(): File {
    // Reflective hack or just make 'themesDir' public in Repository
    // For now assuming we modify ThemeRepository to have:
    // fun getThemesDir(): File = themesDir
    // If not, we can assume:
    return File(this.javaClass.getDeclaredField("themesDir").apply { isAccessible = true }.get(this) as File, "")
}