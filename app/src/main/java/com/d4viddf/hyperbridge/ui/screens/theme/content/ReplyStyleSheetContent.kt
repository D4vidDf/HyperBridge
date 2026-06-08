package com.d4viddf.hyperbridge.ui.screens.theme.content

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.theme.ColorMode
import com.d4viddf.hyperbridge.ui.components.CustomColorBottomSheet
import com.d4viddf.hyperbridge.ui.screens.theme.ThemeViewModel
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReplyStyleSheetContent(viewModel: ThemeViewModel) {
    val isAppOverride = viewModel.editingAppPackage != null

    val backgroundColor = if (isAppOverride) viewModel.appReplyBackgroundColor else viewModel.replyBackgroundColor
    val sendColor = if (isAppOverride) viewModel.appReplySendColor else viewModel.replySendColor
    val textfieldBackgroundColor = if (isAppOverride) viewModel.appReplyTextfieldBackgroundColor else viewModel.replyTextfieldBackgroundColor
    val textColor = if (isAppOverride) viewModel.appReplyTextColor else viewModel.replyTextColor
    val colorMode = if (isAppOverride) viewModel.appReplyColorMode ?: ColorMode.DEFAULT else viewModel.replyColorMode ?: ColorMode.DEFAULT
    val useBlur = if (isAppOverride) viewModel.appReplyUseBlur ?: false else viewModel.replyUseBlur
    val textfieldCornerRadius = if (isAppOverride) viewModel.appReplyTextfieldCornerRadius ?: 24 else viewModel.replyTextfieldCornerRadius
    val sendButtonCornerRadius = if (isAppOverride) viewModel.appReplySendButtonCornerRadius ?: 24 else viewModel.replySendButtonCornerRadius

    var tabIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    androidx.compose.material3.Scaffold(
        containerColor = Color.Transparent,
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.Center,
        floatingActionButton = {
            androidx.compose.material3.HorizontalFloatingToolbar(
                expanded = true,
                content = {
                    Row(
                        modifier = Modifier.wrapContentWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        com.d4viddf.hyperbridge.ui.screens.design.ToolbarOption(
                            selected = tabIndex == 0,
                            icon = Icons.Outlined.Palette,
                            text = stringResource(R.string.creator_nav_colors),
                            onClick = { tabIndex = 0 }
                        )

                        com.d4viddf.hyperbridge.ui.screens.design.ToolbarOption(
                            selected = tabIndex == 1,
                            icon = Icons.Outlined.DisplaySettings,
                            text = stringResource(R.string.reply_appearance),
                            onClick = { tabIndex = 1 }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            // Preview Header pushed above the bottom surface
            Box(modifier = Modifier.weight(1f).padding(top = 16.dp), contentAlignment = Alignment.TopCenter) {
                InlineReplyPreview(
                    isDarkThemePreview = viewModel.isDarkThemePreview,
                    isMaterialYou = colorMode == ColorMode.MATERIAL_YOU,
                    useBlur = useBlur,
                    textfieldCornerRadius = textfieldCornerRadius,
                    sendButtonCornerRadius = sendButtonCornerRadius,
                    textfieldBackgroundColorHex = textfieldBackgroundColor,
                    textColorHex = textColor,
                    sendColorHex = sendColor
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 100.dp)
                    .animateContentSize(alignment = Alignment.BottomCenter),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                androidx.compose.animation.AnimatedContent(
                    targetState = tabIndex,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ReplyTabsTransition",
                    modifier = Modifier.padding(vertical = 12.dp)
                ) { selectedTab ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (selectedTab == 0) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = colorMode == ColorMode.CUSTOM,
                                enter = androidx.compose.animation.expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                                exit = androidx.compose.animation.shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.color_mode_custom),
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                                    )
                                    val isColorsEnabled = !useBlur
                                    ColorPickerSetting(stringResource(R.string.reply_textfield_color), stringResource(R.string.reply_textfield_color_desc), textfieldBackgroundColor, defaultColor = MaterialTheme.colorScheme.surfaceVariant, enabled = isColorsEnabled, onReset = { if (isAppOverride) viewModel.appReplyTextfieldBackgroundColor = null else viewModel.replyTextfieldBackgroundColor = null }) { 
                                        if (isAppOverride) viewModel.appReplyTextfieldBackgroundColor = it else viewModel.replyTextfieldBackgroundColor = it 
                                    }
                                    ColorPickerSetting(stringResource(R.string.reply_text_color), stringResource(R.string.reply_text_color_desc), textColor, defaultColor = MaterialTheme.colorScheme.onSurface, enabled = true, onReset = { if (isAppOverride) viewModel.appReplyTextColor = null else viewModel.replyTextColor = null }) { 
                                        if (isAppOverride) viewModel.appReplyTextColor = it else viewModel.replyTextColor = it 
                                    }
                                    ColorPickerSetting(stringResource(R.string.reply_send_color), stringResource(R.string.reply_send_color_desc), sendColor, defaultColor = MaterialTheme.colorScheme.primary, enabled = true, onReset = { if (isAppOverride) viewModel.appReplySendColor = null else viewModel.replySendColor = null }) { 
                                        if (isAppOverride) viewModel.appReplySendColor = it else viewModel.replySendColor = it 
                                    }
                                    
                                    androidx.compose.material3.HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            Text(
                                text = stringResource(R.string.color_mode),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                            )
                            
                            var showColorModeSheet by remember { mutableStateOf(false) }
                            val modeText = when (colorMode) {
                                ColorMode.DEFAULT -> stringResource(R.string.color_mode_default)
                                ColorMode.APP_ICON -> stringResource(R.string.color_mode_app)
                                ColorMode.MATERIAL_YOU -> stringResource(R.string.color_mode_material)
                                ColorMode.CUSTOM -> stringResource(R.string.color_mode_custom)
                            }
                            
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.color_mode)) },
                                supportingContent = { Text(modeText) },
                                modifier = Modifier
                                    .clickable(role = androidx.compose.ui.semantics.Role.DropdownList) { showColorModeSheet = true },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = stringResource(R.string.color_mode_change_cd)
                                    )
                                }
                            )
                            
                            if (showColorModeSheet) {
                                androidx.compose.material3.ModalBottomSheet(
                                    onDismissRequest = { showColorModeSheet = false },
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                                        Text(stringResource(R.string.color_mode_select), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                                        ColorMode.entries.forEach { mode ->
                                            val label = when (mode) {
                                                ColorMode.DEFAULT -> stringResource(R.string.color_mode_default)
                                                ColorMode.APP_ICON -> stringResource(R.string.color_mode_app)
                                                ColorMode.MATERIAL_YOU -> stringResource(R.string.color_mode_material)
                                                ColorMode.CUSTOM -> stringResource(R.string.color_mode_custom)
                                            }
                                            val desc = when (mode) {
                                                ColorMode.DEFAULT -> stringResource(R.string.color_mode_default_desc)
                                                ColorMode.APP_ICON -> stringResource(R.string.color_mode_app_desc)
                                                ColorMode.MATERIAL_YOU -> stringResource(R.string.color_mode_material_desc)
                                                ColorMode.CUSTOM -> stringResource(R.string.color_mode_custom_desc)
                                            }
                                            ListItem(
                                                headlineContent = { Text(label) },
                                                supportingContent = { Text(desc) },
                                                modifier = Modifier.clickable {
                                                    if (isAppOverride) viewModel.appReplyColorMode = mode else viewModel.replyColorMode = mode
                                                    showColorModeSheet = false
                                                },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                                trailingContent = {
                                                    if (colorMode == mode) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.reply_appearance),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                            )
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = { Text(stringResource(R.string.reply_blur_effect)) },
                                supportingContent = { Text(stringResource(R.string.reply_blur_effect_desc)) },
                                trailingContent = { Switch(checked = useBlur, onCheckedChange = { if (isAppOverride) viewModel.appReplyUseBlur = it else viewModel.replyUseBlur = it }) }
                            )
                            SliderSetting(stringResource(R.string.reply_textfield_radius), textfieldCornerRadius.toFloat(), 0f..64f) { 
                                if (isAppOverride) viewModel.appReplyTextfieldCornerRadius = it.toInt() else viewModel.replyTextfieldCornerRadius = it.toInt() 
                            }
                            SliderSetting(stringResource(R.string.reply_send_radius), sendButtonCornerRadius.toFloat(), 0f..64f) { 
                                if (isAppOverride) viewModel.appReplySendButtonCornerRadius = it.toInt() else viewModel.replySendButtonCornerRadius = it.toInt() 
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSetting(title: String, subtitle: String, colorHex: String?, defaultColor: Color, enabled: Boolean = true, onReset: (() -> Unit)? = null, onColorChanged: (String) -> Unit) {
    var showColorPicker by remember { mutableStateOf(false) }
    val color = colorHex?.let { safeParseColor(it) } ?: defaultColor
    
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onReset != null && colorHex != null) {
                    IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Reset", modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (enabled) color else color.copy(alpha = 0.38f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
            }
        },
        modifier = Modifier.clickable(enabled = enabled) { showColorPicker = true },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            supportingColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        )
    )
    
    if (showColorPicker) {
        CustomColorBottomSheet(
            initialColor = color,
            onDismiss = { showColorPicker = false },
            onColorAdded = { newColor ->
                val hex = String.format("#%08X", newColor.toArgb())
                onColorChanged(hex)
                showColorPicker = false
            }
        )
    }
}

@Composable
fun SliderSetting(title: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title)
            Text(value.toInt().toString())
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
fun InlineReplyPreview(
    isDarkThemePreview: Boolean,
    isMaterialYou: Boolean,
    useBlur: Boolean,
    textfieldCornerRadius: Int,
    sendButtonCornerRadius: Int,
    textfieldBackgroundColorHex: String?,
    textColorHex: String?,
    sendColorHex: String?
) {
    val tfBgColor = if (isMaterialYou) MaterialTheme.colorScheme.surfaceVariant else (textfieldBackgroundColorHex?.let { safeParseColor(it) } ?: MaterialTheme.colorScheme.surfaceVariant)
    val sendColor = if (isMaterialYou) MaterialTheme.colorScheme.primary else (sendColorHex?.let { safeParseColor(it) } ?: MaterialTheme.colorScheme.primary)
    val txtColor = if (isMaterialYou) MaterialTheme.colorScheme.onSurface else (textColorHex?.let { safeParseColor(it) } ?: MaterialTheme.colorScheme.onSurface)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        val hazeState = remember { dev.chrisbanes.haze.HazeState() }
        
        // A placeholder background simulating screen content
        Box(modifier = Modifier
            .matchParentSize()
            .then(if (useBlur) Modifier.haze(state = hazeState) else Modifier)
        )

        // Content overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(Color.Transparent),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val tfHazeModifier = if (useBlur) {
                    Modifier.hazeChild(state = hazeState, shape = RoundedCornerShape(textfieldCornerRadius.dp), style = dev.chrisbanes.haze.HazeStyle(blurRadius = 25.dp, backgroundColor = Color.Transparent, tint = dev.chrisbanes.haze.HazeTint(Color.Black.copy(alpha = 0.2f))))
                } else Modifier

                val btnHazeModifier = if (useBlur) {
                    Modifier.hazeChild(state = hazeState, shape = RoundedCornerShape(sendButtonCornerRadius.dp), style = dev.chrisbanes.haze.HazeStyle(blurRadius = 25.dp, backgroundColor = Color.Transparent, tint = dev.chrisbanes.haze.HazeTint(Color.Black.copy(alpha = 0.2f))))
                } else Modifier
                
                Box(modifier = Modifier
                    .weight(1f)
                    .then(
                        Modifier.heightIn(
                            min = 56.dp,
                            max = 200.dp
                        )
                    )
                ) {
                    // Blurred Background Layer
                    Box(modifier = Modifier
                        .matchParentSize()
                        .then(tfHazeModifier)
                        .background(if (useBlur) tfBgColor.copy(alpha = 0.5f) else tfBgColor, RoundedCornerShape(textfieldCornerRadius.dp))
                    )
                    
                    // Content Layer (Sharp)
                    TextField(
                        value = stringResource(R.string.reply_hint),
                        onValueChange = { },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = txtColor),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        maxLines = 4,
                        enabled = false
                    )
                }
                
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(56.dp)
                ) {
                    // Blurred Background Layer
                    Box(modifier = Modifier
                        .matchParentSize()
                        .then(btnHazeModifier)
                        .background(if (useBlur) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(sendButtonCornerRadius.dp))
                    )
                    
                    // Content Layer (Sharp)
                    IconButton(
                        onClick = { },
                        enabled = true,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = sendColor
                        )
                    }
                }
            }
        }
    }
}
