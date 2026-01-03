package com.d4viddf.hyperbridge.ui.screens.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R


// --- CALLS & META ---
@Composable
fun MetaDetailContent(viewModel: ThemeViewModel) {
    val fm = LocalFocusManager.current
    Column(Modifier.padding(24.dp)) {
        OutlinedTextField(viewModel.themeName, { viewModel.themeName = it }, label = { Text(stringResource(R.string.meta_label_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardActions = KeyboardActions(onDone = { fm.clearFocus() }))
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(viewModel.themeAuthor, { viewModel.themeAuthor = it }, label = { Text(stringResource(R.string.meta_label_author)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardActions = KeyboardActions(onDone = { fm.clearFocus() }))
    }
}