package com.d4viddf.hyperbridge.ui.components.island

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.IslandTemplateCatalog
import com.d4viddf.hyperbridge.models.translator.PillLeftDesign
import com.d4viddf.hyperbridge.models.translator.PillRightDesign
import com.d4viddf.hyperbridge.models.translator.ProgressSlotType
import com.d4viddf.hyperbridge.ui.screens.theme.getShapeFromId
import com.d4viddf.hyperbridge.ui.screens.theme.safeParseColor

enum class IslandPreviewMode {
    EXPANDED, COMPACT_PILL
}

/**
 * The stand-in notification a preview interpolates its templates against. Defaults keep the
 * ride-share example the editor has always shown; the template gallery passes its own example so
 * each of the ten layouts previews with content that makes sense for it.
 */
data class IslandPreviewSample(
    val title: String = "Ride Arriving Soon",
    val text: String = "Driver is 2 minutes away (Toyota Camry)",
    val subtext: String = "License: ABC-1234",
    val sender: String = "Alex",
    val conversation: String = "Trip Updates",
    val app: String = "RideApp",
    val track: String = "Never Gonna Give You Up",
    val artist: String = "Rick Astley",
    val progressPercent: Int = 65,
    val timerText: String = "04:25",
    val countdownText: String = "00:05"
)

private fun String.withSample(sample: IslandPreviewSample): String = this
    .replace("{media.track}", sample.track)
    .replace("{media.artist}", sample.artist)
    .replace("{notif.title}", sample.title)
    .replace("{notif.text}", sample.text)
    .replace("{notif.subtext}", sample.subtext)
    .replace("{notif.sender}", sample.sender)
    .replace("{notif.conversation}", sample.conversation)
    .replace("{notif.app}", sample.app)
    .replace("{app.name}", sample.app)
    .replace("{regex.1}", sample.subtext)

/**
 * Top-Level Realistic HyperOS 3 Island Previewer
 * Seamlessly toggles between the Expanded Dynamic Island (Focus Notification)
 * and the Compact Status Bar Pill with live variable interpolation and theme styling.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HyperOsIslandPreview(
    modifier: Modifier = Modifier,
    translator: CustomTranslator,
    installedThemes: List<HyperTheme> = emptyList(),
    initialMode: IslandPreviewMode = IslandPreviewMode.EXPANDED,
    sample: IslandPreviewSample = IslandPreviewSample(),
    showChrome: Boolean = true
) {
    var previewMode by remember { mutableStateOf(initialMode) }

    // A TEMPLATE translator may carry nothing but its templateId, so resolve the preset first and
    // render from that: the gallery, the design cards and the editor all see the same island.
    val presentation = IslandTemplateCatalog.effectivePresentation(translator.presentation)

    // 1. Resolve Effective Theme Styling
    val linkedTheme = if (translator.themeBinding.themeId.isNotBlank() && translator.themeBinding.themeId != "active") {
        installedThemes.find { it.id == translator.themeBinding.themeId }
    } else {
        null
    }

    val highlightColor = if (translator.themeBinding.overrideHighlightColor != null) {
        safeParseColor(translator.themeBinding.overrideHighlightColor)
    } else if (linkedTheme?.global?.highlightColor != null) {
        safeParseColor(linkedTheme.global.highlightColor)
    } else {
        MaterialTheme.colorScheme.primary
    }

    val textColor = if (linkedTheme?.global?.textColor != null) {
        safeParseColor(linkedTheme.global.textColor)
    } else {
        Color.White
    }

    val iconShapeId = if (translator.themeBinding.iconShapeId.isNotBlank() && translator.themeBinding.iconShapeId != "circle") {
        translator.themeBinding.iconShapeId
    } else {
        linkedTheme?.global?.iconShapeId ?: "circle"
    }
    val iconShape = getShapeFromId(iconShapeId).toShape()

    val progressColor = if (linkedTheme?.defaultProgress?.activeColor != null) {
        safeParseColor(linkedTheme.defaultProgress.activeColor)
    } else {
        highlightColor
    }

    val buttonPaddingPercent = linkedTheme?.global?.iconPaddingPercent ?: translator.themeBinding.iconPaddingPercent
    val buttonBackgroundColor = if (linkedTheme?.defaultActions?.get("default")?.backgroundColor != null) {
        safeParseColor(linkedTheme.defaultActions["default"]?.backgroundColor!!)
    } else {
        Color(0xFF262626)
    }
    val buttonTextColor = if (linkedTheme?.defaultActions?.get("default")?.textColor != null) {
        safeParseColor(linkedTheme.defaultActions["default"]?.textColor!!)
    } else {
        Color.White
    }

    // 2. Interpolate Dynamic Values (Supporting both Visual Slots and RAW_PARAM_V2)
    val isRawMode = presentation.mode == com.d4viddf.hyperbridge.models.translator.PresentationMode.RAW_PARAM_V2
    val rawJson = presentation.rawParamV2?.jsonTemplate ?: ""

    var titleText = presentation.textSlot.titleTemplate
    var subtitleText = presentation.textSlot.subtitleTemplate
    var highlightText = presentation.textSlot.highlightTextTemplate
    var parsedHighlightColor = highlightColor
    var pillTitle = titleText
    var pillRightText = if (presentation.progressSlot.type == ProgressSlotType.TIMER) sample.timerText else sample.countdownText
    var hasProgress = presentation.progressSlot.type != ProgressSlotType.NONE && presentation.progressSlot.type != ProgressSlotType.TIMER
    var progressPercent = sample.progressPercent

    if (isRawMode && rawJson.isNotBlank()) {
        try {
            val element = com.google.gson.JsonParser.parseString(rawJson)
            val root = if (element.isJsonObject) {
                val obj = element.asJsonObject
                if (obj.has("param_v2")) obj.getAsJsonObject("param_v2") else obj
            } else null

            if (root != null) {
                val island = root.getAsJsonObject("param_island")
                val bigIsland = island?.getAsJsonObject("bigIslandArea")
                val leftInfo = bigIsland?.getAsJsonObject("imageTextInfoLeft")
                val textInfo = leftInfo?.getAsJsonObject("textInfo") ?: leftInfo?.getAsJsonObject("miui.focus.paramtextInfo")
                val baseInfo = root.getAsJsonObject("baseInfo")

                val extractedTitle = textInfo?.get("title")?.asString
                    ?: baseInfo?.get("title")?.asString
                    ?: root.get("ticker")?.asString
                val extractedContent = textInfo?.get("content")?.asString
                    ?: baseInfo?.get("content")?.asString
                val extractedFront = textInfo?.get("frontTitle")?.asString

                val rawHighlight = island?.get("highlightColor")?.asString
                if (!rawHighlight.isNullOrBlank()) {
                    parsedHighlightColor = safeParseColor(rawHighlight)
                }

                if (!extractedTitle.isNullOrBlank()) {
                    titleText = extractedTitle
                    pillTitle = extractedTitle
                }
                if (!extractedContent.isNullOrBlank()) {
                    subtitleText = extractedContent
                }
                if (!extractedFront.isNullOrBlank()) {
                    highlightText = extractedFront
                }

                val rightInfo = bigIsland?.getAsJsonObject("imageTextInfoRight")
                val rightText = rightInfo?.getAsJsonObject("textInfo")?.get("title")?.asString
                if (!rightText.isNullOrBlank()) {
                    pillRightText = rightText
                }

                val progressInfo = bigIsland?.getAsJsonObject("progressInfo")
                if (progressInfo != null) {
                    hasProgress = true
                    progressPercent = progressInfo.get("progress")?.asInt ?: sample.progressPercent
                }
            }
        } catch (_: Exception) {}
    }

    titleText = titleText.withSample(sample)
    subtitleText = subtitleText.withSample(sample)
    highlightText = highlightText?.withSample(sample)
    pillTitle = pillTitle.withSample(sample)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mode Selector: Connected Button Group Acting as Tabs. Hidden when the preview is
            // used as a thumbnail (template gallery, design cards), where there is nothing to switch.
            if (showChrome) Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    val modes = listOf(
                        IslandPreviewMode.EXPANDED to stringResource(R.string.translator_preview_mode_expanded),
                        IslandPreviewMode.COMPACT_PILL to stringResource(R.string.translator_preview_mode_pill)
                    )
                    modes.forEachIndexed { index, (mode, label) ->
                        val isSelected = previewMode == mode
                        val shape = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }
                        ToggleButton(
                            checked = isSelected,
                            onCheckedChange = { previewMode = mode },
                            shapes = shape,
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
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

                // HyperOS 3 Badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "HyperOS 3",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Preview Canvas (Maintains stable height matching expanded island so layout below does not shift)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (showChrome) 175.dp else 150.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141414))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status Bar Layer (Time on left, Compact Pill in center, Icons on right)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Left: Status Bar Clock
                        Text(
                            text = "12:00",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )

                        // Center: Compact Pill (When in COMPACT_PILL mode)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = previewMode == IslandPreviewMode.COMPACT_PILL,
                            enter = fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.85f),
                            exit = fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.85f),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            HyperOsCompactPill(
                                leftDesign = presentation.pill.leftDesign,
                                rightDesign = presentation.pill.rightDesign,
                                title = pillTitle,
                                rightText = pillRightText,
                                progressPercent = progressPercent,
                                hasProgress = hasProgress,
                                highlightColor = parsedHighlightColor,
                                iconShape = iconShape
                            )
                        }

                        // Right: Signal & Battery Icons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(Icons.Default.SignalCellular4Bar, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(12.dp))
                            Icon(Icons.Default.BatteryFull, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(12.dp))
                        }
                    }

                    // Expanded Island (Animates smoothly underneath the status bar)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = previewMode == IslandPreviewMode.EXPANDED,
                        enter = fadeIn() + androidx.compose.animation.expandVertically(expandFrom = Alignment.Top),
                        exit = fadeOut() + androidx.compose.animation.shrinkVertically(shrinkTowards = Alignment.Top)
                    ) {
                        HyperOsExpandedIsland {
                            // Row 1: Graphic + Text
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                HyperOsLeftGraphic(
                                    source = presentation.leftSlot.source,
                                    highlightColor = parsedHighlightColor,
                                    iconShape = iconShape
                                )

                                HyperOsTextGroup(
                                    title = titleText,
                                    subtitle = subtitleText,
                                    highlightText = highlightText,
                                    highlightColor = parsedHighlightColor,
                                    textColor = textColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Row 2: Progress (if configured)
                            if (hasProgress || presentation.progressSlot.type != ProgressSlotType.NONE) {
                                Spacer(Modifier.height(2.dp))
                                HyperOsProgressBar(
                                    progressPercent = progressPercent,
                                    progressSlot = presentation.progressSlot,
                                    highlightColor = progressColor
                                )
                            }

                            // Row 3: Action Buttons (if configured)
                            if (presentation.actionSlots.any { it.isVisible }) {
                                Spacer(Modifier.height(4.dp))
                                HyperOsActionButtons(
                                    actionSlots = presentation.actionSlots,
                                    highlightColor = parsedHighlightColor,
                                    buttonShape = iconShape,
                                    buttonPaddingPercent = buttonPaddingPercent,
                                    buttonBackgroundColor = buttonBackgroundColor,
                                    buttonTextColor = buttonTextColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "HyperOS Island Preview Interactive", showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun HyperOsIslandPreviewComponentPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        HyperOsIslandPreview(
            translator = CustomTranslator(
                id = "preview-food-delivery",
                meta = com.d4viddf.hyperbridge.models.translator.TranslatorMetadata(
                    name = "Food Delivery",
                    description = "Ride and food tracker"
                ),
                presentation = com.d4viddf.hyperbridge.models.translator.PresentationConfig(
                    textSlot = com.d4viddf.hyperbridge.models.translator.TextSlotConfig(
                        titleTemplate = "Courier arriving soon",
                        subtitleTemplate = "2 km away • 8 mins",
                        highlightTextTemplate = "Live"
                    ),
                    progressSlot = com.d4viddf.hyperbridge.models.translator.ProgressSlotConfig(
                        type = ProgressSlotType.PROGRESS_BAR,
                        showPercentage = true
                    ),
                    pill = com.d4viddf.hyperbridge.models.translator.CompactPillConfig(
                        leftDesign = PillLeftDesign.ICON_AND_TEXT,
                        rightDesign = PillRightDesign.TIMER
                    )
                )
            )
        )
    }
}
