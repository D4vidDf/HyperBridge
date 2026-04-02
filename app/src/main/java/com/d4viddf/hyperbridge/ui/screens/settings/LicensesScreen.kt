package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.d4viddf.hyperbridge.R

data class Library(val name: String, val author: String, val license: String, val url: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    val libs = listOf(
        Library("HyperIsland-ToolKit", "D4vidDf", "Apache 2.0", "https://github.com/D4vidDf/HyperIsland-ToolKit"),
        Library("Jetpack Compose", "Google", "Apache 2.0", "https://developer.android.com/jetpack/compose"),
        Library("Material 3", "Google", "Apache 2.0", "https://m3.material.io/"),
        Library("AndroidX Core", "Google", "Apache 2.0", "https://developer.android.com/jetpack/androidx"),
        Library("AndroidX Activity", "Google", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/activity"),
        Library("AndroidX AppCompat", "Google", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/appcompat"),
        Library("AndroidX DataStore", "Google", "Apache 2.0", "https://developer.android.com/topic/libraries/architecture/datastore"),
        Library("AndroidX Lifecycle", "Google", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/lifecycle"),
        Library("AndroidX Navigation 3", "Google", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/navigation"),
        Library("AndroidX Palette", "Google", "Apache 2.0", "https://developer.android.com/develop/ui/views/graphics/palette"),
        Library("AndroidX Room", "Google", "Apache 2.0", "https://developer.android.com/training/data-storage/room"),
        Library("Gson", "Google", "Apache 2.0", "https://github.com/google/gson"),
        Library("Kotlin Coroutines", "JetBrains", "Apache 2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
        Library("Kotlin Serialization", "JetBrains", "Apache 2.0", "https://github.com/Kotlin/kotlinx.serialization")
    ).sortedBy { it.name }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.open_source_licenses)) },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(libs) { lib ->
                ListItem(
                    headlineContent = { Text(lib.name, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("${lib.author} • ${lib.license}") },
                    modifier = Modifier.clickable { uriHandler.openUri(lib.url) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            }
        }
    }
}
