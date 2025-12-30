package com.d4viddf.hyperbridge.ui.screens.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R


// --- CALLS & META ---

@Composable
fun CallStyleSheetContent(viewModel: ThemeViewModel) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text(stringResource(R.string.calls_label_answer), style = MaterialTheme.typography.titleSmall, color = safeParseColor("#34C759"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssetPickerButton("", Icons.Rounded.Call) { uri -> viewModel.stageAsset("call_answer", uri); viewModel.callAnswerUri = uri }
            Spacer(Modifier.width(16.dp))
            OutlinedTextField(value = viewModel.callAnswerColor, onValueChange = { viewModel.callAnswerColor = it }, label = { Text(stringResource(R.string.calls_label_hex)) }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.calls_label_decline), style = MaterialTheme.typography.titleSmall, color = safeParseColor("#FF3B30"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssetPickerButton("", Icons.Rounded.CallEnd) { uri -> viewModel.stageAsset("call_decline", uri); viewModel.callDeclineUri = uri }
            Spacer(Modifier.width(16.dp))
            OutlinedTextField(value = viewModel.callDeclineColor, onValueChange = { viewModel.callDeclineColor = it }, label = { Text(stringResource(R.string.calls_label_hex)) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun MetaDetailContent(viewModel: ThemeViewModel) {
    val fm = LocalFocusManager.current
    Column(Modifier.padding(24.dp)) {
        OutlinedTextField(viewModel.themeName, { viewModel.themeName = it }, label = { Text(stringResource(R.string.meta_label_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardActions = KeyboardActions(onDone = { fm.clearFocus() }))
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(viewModel.themeAuthor, { viewModel.themeAuthor = it }, label = { Text(stringResource(R.string.meta_label_author)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardActions = KeyboardActions(onDone = { fm.clearFocus() }))
    }
}