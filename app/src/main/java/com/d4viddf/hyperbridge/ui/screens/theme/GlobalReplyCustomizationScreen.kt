package com.d4viddf.hyperbridge.ui.screens.theme

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.screens.theme.content.ReplyStyleSheetContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalReplyCustomizationScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ThemeViewModel = viewModel()
    val activeThemeId by viewModel.activeThemeId.collectAsState()
    
    LaunchedEffect(activeThemeId) {
        if (activeThemeId != null) {
            viewModel.loadThemeForEditing(activeThemeId!!)
        } else {
            viewModel.clearCreatorState()
        }
    }

    BackHandler {
        viewModel.saveTheme(existingId = viewModel.currentEditingThemeId, apply = true)
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.inline_reply_title)) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = {
                        viewModel.saveTheme(existingId = viewModel.currentEditingThemeId, apply = true)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (viewModel.currentEditingThemeId != null) {
                ReplyStyleSheetContent(viewModel = viewModel)
            }
        }
    }
}
