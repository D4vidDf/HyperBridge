package com.d4viddf.hyperbridge.ui.components.island

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d4viddf.hyperbridge.models.translator.ActionDisplayMode
import com.d4viddf.hyperbridge.models.translator.ActionSlotConfig
import com.d4viddf.hyperbridge.models.translator.ActionSource
import com.d4viddf.hyperbridge.models.translator.PillLeftDesign
import com.d4viddf.hyperbridge.models.translator.PillRightDesign
import com.d4viddf.hyperbridge.models.translator.ProgressSlotConfig
import com.d4viddf.hyperbridge.models.translator.ProgressSlotType
import com.d4viddf.hyperbridge.models.translator.SmartActionType

// ==========================================
// 1. HYPEROS 3 EXPANDED ISLAND CONTAINER
// ==========================================

/**
 * HyperOS 3 Expanded Island Base Card
 * Replicates the pure black OLED canvas, 28dp smooth squircle, top center camera indicator slot,
 * and standard padding specified in the Xiaomi HyperOS 3 developer documentation.
 */
@Composable
fun HyperOsExpandedIsland(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2B2B2B), Color(0xFF141414))
            ),
            width = 1.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content()

            // Bottom Drag Handle / Pill Bar indicator (matching real HyperOS Focus Island UI)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF333333))
                )
            }
        }
    }
}

// ==========================================
// 2. IDENTIFICATION GRAPHIC (LEFT COMPONENT)
// ==========================================

/**
 * Replicates HyperOS 3 Identification Graphics:
 * - App Icon (with theme squircle/shape)
 * - User Profile Avatar (48dp with circular clip and sub-badge)
 * - Large Artwork / Cover
 */
@Composable
fun HyperOsLeftGraphic(
    source: String,
    highlightColor: Color,
    iconShape: Shape,
    modifier: Modifier = Modifier
) {
    when (source.uppercase()) {
        "AVATAR" -> {
            Box(modifier = modifier.size(48.dp)) {
                // Main Avatar Circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF333333)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                // Mini App Badge at Bottom-Right
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .padding(1.5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(highlightColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(9.dp)
                        )
                    }
                }
            }
        }
        "HIDDEN" -> {
            // Hidden - does not occupy space
        }
        else -> {
            // Standard App Icon Graphic (44dp with custom squircle shape & highlight container)
            Box(
                modifier = modifier
                    .size(44.dp)
                    .clip(iconShape)
                    .background(highlightColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

// ==========================================
// 3. TEXT GROUP COMPONENT
// ==========================================

/**
 * Replicates HyperOS 3 Text Component specifications:
 * - Primary Text (Title): Bold 15sp, max 1-2 lines, high contrast.
 * - Secondary Text (Subtitle): 13sp with 75% opacity and optional prefix separator or icon.
 * - Highlight Badge / Special Tag: Rounded pill tag with theme background.
 */
@Composable
fun HyperOsTextGroup(
    title: String,
    subtitle: String,
    highlightText: String? = null,
    highlightColor: Color = Color(0xFF3482FF),
    textColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title.ifBlank { "Notification Title" },
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (!highlightText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = highlightColor.copy(alpha = 0.2f),
                    modifier = Modifier.padding(start = 2.dp)
                ) {
                    Text(
                        text = highlightText,
                        color = highlightColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (subtitle.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(textColor.copy(alpha = 0.4f))
                )
                Text(
                    text = subtitle,
                    color = textColor.copy(alpha = 0.72f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ==========================================
// 4. PROGRESS COMPONENT (HYPEROS 3 SPEC)
// ==========================================

/**
 * Replicates HyperOS 3 Progress Component (Template 4 / 5 / 6 / 7):
 * - Linear smooth progress bar with highlight or gradient track.
 * - Waypoints / Multi-stage dots or destination icon.
 * - Percentage or status text counter.
 */
@Composable
fun HyperOsProgressBar(
    progressPercent: Int = 65,
    progressSlot: ProgressSlotConfig,
    highlightColor: Color = Color(0xFF3482FF),
    modifier: Modifier = Modifier
) {
    if (progressSlot.type == ProgressSlotType.NONE) return

    val clampedProgress = progressPercent.coerceIn(0, 100)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when (progressSlot.type) {
            ProgressSlotType.PROGRESS_BAR -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Linear Track
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF2A2A2A))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(clampedProgress / 100f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(highlightColor.copy(alpha = 0.8f), highlightColor)
                                    )
                                )
                        )
                    }

                    if (progressSlot.showPercentage) {
                        Text(
                            text = "$clampedProgress%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            ProgressSlotType.WAYPOINT -> {
                // Waypoint Multi-stage track (Delivery / Ride hail HyperOS 3 spec)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF2A2A2A))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(clampedProgress / 100f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(highlightColor)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(highlightColor))
                            Text("Pickup", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (clampedProgress >= 50) highlightColor else Color(0xFF555555)))
                            Text("On Way", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.NearMe, contentDescription = null, tint = if (clampedProgress >= 100) highlightColor else Color(0xFF555555), modifier = Modifier.size(10.dp))
                            Text("Arrived", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                    }
                }
            }
            ProgressSlotType.TIMER -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Elapsed Time",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "04:25",
                        color = highlightColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// 5. ACTION BUTTONS ROW (HYPEROS 3 SPEC)
// ==========================================

/**
 * Replicates HyperOS 3 Action Components (Buttons 1, 2, 3, 4, 5):
 * - Primary Pill Button (Accent background or white on black)
 * - Secondary Circular Buttons (36dp glassmorphism / dark grey background)
 * - Inline Reply Pill
 */
@Composable
fun HyperOsActionButtons(
    actionSlots: List<ActionSlotConfig>,
    highlightColor: Color = Color(0xFF3482FF),
    buttonShape: Shape = CircleShape,
    buttonPaddingPercent: Int = 15,
    buttonBackgroundColor: Color = Color(0xFF262626),
    buttonTextColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val visibleSlots = actionSlots.filter { it.isVisible }.sortedBy { it.slotPosition }
    if (visibleSlots.isEmpty()) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        visibleSlots.forEachIndexed { index, slot ->
            val isPrimary = index == 0 && visibleSlots.size > 1 && slot.displayMode != ActionDisplayMode.ICON_ONLY

            val iconVector: ImageVector = when (slot.source) {
                ActionSource.INLINE_REPLY -> Icons.AutoMirrored.Filled.Reply
                ActionSource.SMART_ACTION -> when (slot.smartActionType) {
                    SmartActionType.OTP_COPY -> Icons.Default.ContentCopy
                    SmartActionType.OPEN_URL -> Icons.Default.OpenInBrowser
                    SmartActionType.DIAL_NUMBER -> Icons.Default.Phone
                    else -> Icons.Outlined.AutoAwesome
                }
                else -> Icons.Default.PlayArrow
            }

            val label = slot.customLabel ?: when (slot.source) {
                ActionSource.INLINE_REPLY -> "Reply"
                ActionSource.SMART_ACTION -> when (slot.smartActionType) {
                    SmartActionType.OTP_COPY -> "Copy OTP"
                    SmartActionType.OPEN_URL -> "Open Link"
                    SmartActionType.DIAL_NUMBER -> "Call"
                    else -> "Action"
                }
                else -> "Action ${index + 1}"
            }

            when (slot.displayMode) {
                ActionDisplayMode.ICON_ONLY -> {
                    // Icon Action Button (Applies theme shape, color, and padding)
                    val bg = if (isPrimary) highlightColor else buttonBackgroundColor
                    val tint = if (isPrimary) Color.White else buttonTextColor
                    val innerPadding = (38 * (buttonPaddingPercent / 100f)).coerceAtLeast(6f).dp

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(buttonShape)
                            .background(bg)
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            iconVector,
                            contentDescription = label,
                            tint = tint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                ActionDisplayMode.TEXT_ONLY -> {
                    // Text Pill / Styled Button
                    val bg = if (isPrimary) highlightColor else buttonBackgroundColor
                    val txtColor = if (isPrimary) Color.White else buttonTextColor

                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(50))
                            .background(bg)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = txtColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                ActionDisplayMode.ICON_AND_TEXT -> {
                    // Full Feature Pill / Styled Button
                    val bg = if (isPrimary) highlightColor else buttonBackgroundColor
                    val txtColor = if (isPrimary) Color.White else buttonTextColor

                    Row(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(50))
                            .background(bg)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            iconVector,
                            contentDescription = null,
                            tint = txtColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = label,
                            color = txtColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. COMPACT STATUS BAR PILL (HYPEROS 3 SPEC)
// ==========================================

/**
 * Fading text helper for Compact Pill:
 * When text is long/overflowing, fades out the edge closest to the central camera cutout.
 * - Left side of camera: right edge fades out towards the camera cutout.
 * - Right side of camera: left edge fades out towards the camera cutout.
 */
@Composable
private fun HyperOsPillFadingText(
    text: String,
    color: Color,
    fontSize: TextUnit = 12.sp,
    fontWeight: FontWeight = FontWeight.Medium,
    isLeftSide: Boolean = true,
    maxTextWidth: Dp = 80.dp,
    modifier: Modifier = Modifier
) {
    var hasOverflow by remember(text, maxTextWidth) { mutableStateOf(false) }

    val fadeModifier = if (hasOverflow) {
        Modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                val fadeLengthPx = 14.dp.toPx()
                if (size.width > 0f && fadeLengthPx > 0f) {
                    val brush = if (isLeftSide) {
                        val fadeStart = (size.width - fadeLengthPx).coerceAtLeast(0f) / size.width
                        Brush.horizontalGradient(
                            0f to Color.Black,
                            fadeStart to Color.Black,
                            1f to Color.Transparent
                        )
                    } else {
                        val fadeEnd = fadeLengthPx.coerceAtMost(size.width) / size.width
                        Brush.horizontalGradient(
                            0f to Color.Transparent,
                            fadeEnd to Color.Black,
                            1f to Color.Black
                        )
                    }
                    drawRect(brush = brush, blendMode = BlendMode.DstIn)
                }
            }
    } else {
        Modifier
    }

    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
            hasOverflow = result.hasVisualOverflow || result.didOverflowWidth
        },
        modifier = modifier
            .widthIn(max = maxTextWidth)
            .then(fadeModifier)
    )
}

/**
 * Replicates HyperOS 3 Compact Pill Island in status bar:
 * Uses symmetrical punch-hole geometry with left and right balancing.
 * Wraps content symmetrically, clamping width and fading text near the camera cutout when long.
 */
@Composable
fun HyperOsCompactPill(
    leftDesign: PillLeftDesign,
    rightDesign: PillRightDesign,
    title: String = "App Alert",
    rightText: String = "00:05",
    progressPercent: Int = 65,
    hasProgress: Boolean = false,
    highlightColor: Color = Color(0xFF3482FF),
    iconShape: Shape = CircleShape,
    modifier: Modifier = Modifier
) {
    val horizontalPaddingPx = with(LocalDensity.current) { 10.dp.roundToPx() }
    val cameraGapPx = with(LocalDensity.current) { 6.dp.roundToPx() }
    val minSideWidthPx = with(LocalDensity.current) { 14.dp.roundToPx() }
    val maxSideWidthPx = with(LocalDensity.current) { 52.dp.roundToPx() }
    val pillHeightPx = with(LocalDensity.current) { 34.dp.roundToPx() }

    val showProgressCircle = when (rightDesign) {
        PillRightDesign.PROGRESS_PERCENT -> true
        PillRightDesign.AUTO -> hasProgress
        else -> false
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Layout(
            content = {
                // Measurable 0: Left Content
                Box(contentAlignment = Alignment.CenterStart) {
                    when (leftDesign) {
                        PillLeftDesign.ICON_ONLY -> {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(iconShape)
                                    .background(highlightColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        PillLeftDesign.ICON_AND_TEXT -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.widthIn(max = 52.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(iconShape)
                                        .background(highlightColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                HyperOsPillFadingText(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    isLeftSide = true,
                                    maxTextWidth = 32.dp
                                )
                            }
                        }
                        PillLeftDesign.TEXT_ONLY -> {
                            HyperOsPillFadingText(
                                text = title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                isLeftSide = true,
                                maxTextWidth = 52.dp
                            )
                        }
                        PillLeftDesign.AVATAR -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.widthIn(max = 52.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(highlightColor.copy(alpha = 0.8f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                HyperOsPillFadingText(
                                    text = "Alice",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    isLeftSide = true,
                                    maxTextWidth = 32.dp
                                )
                            }
                        }
                        PillLeftDesign.HIDDEN -> {}
                    }
                }

                // Measurable 1: Camera Punch-Hole Mock
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B1B1B)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0D0D0D))
                    )
                }

                // Measurable 2: Right Content
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (showProgressCircle) {
                        Box(
                            modifier = Modifier.size(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { (progressPercent.coerceIn(0, 100) / 100f) },
                                modifier = Modifier.size(16.dp),
                                color = highlightColor,
                                trackColor = highlightColor.copy(alpha = 0.25f),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "$progressPercent",
                                color = Color.White,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        when (rightDesign) {
                            PillRightDesign.AUTO, PillRightDesign.TIMER -> {
                                HyperOsPillFadingText(
                                    text = rightText,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    isLeftSide = false,
                                    maxTextWidth = 46.dp
                                )
                            }
                            PillRightDesign.HIGHLIGHT_TEXT -> {
                                HyperOsPillFadingText(
                                    text = "Done",
                                    color = highlightColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    isLeftSide = false,
                                    maxTextWidth = 46.dp
                                )
                            }
                            PillRightDesign.NONE -> {}
                            PillRightDesign.PROGRESS_PERCENT -> {} // handled above
                        }
                    }
                }
            }
        ) { measurables, constraints ->
            val unconstrained = constraints.copy(minWidth = 0, minHeight = 0)
            val leftPlaceable = measurables[0].measure(unconstrained)
            val cameraPlaceable = measurables[1].measure(unconstrained)
            val rightPlaceable = measurables[2].measure(unconstrained)

            val sideWidth = maxOf(leftPlaceable.width, rightPlaceable.width, minSideWidthPx).coerceAtMost(maxSideWidthPx)
            val totalWidth = (horizontalPaddingPx * 2) + (sideWidth * 2) + cameraPlaceable.width + (cameraGapPx * 2)
            val totalHeight = pillHeightPx

            layout(totalWidth, totalHeight) {
                leftPlaceable.placeRelative(
                    x = horizontalPaddingPx,
                    y = (totalHeight - leftPlaceable.height) / 2
                )
                val cameraX = horizontalPaddingPx + sideWidth + cameraGapPx
                cameraPlaceable.placeRelative(
                    x = cameraX,
                    y = (totalHeight - cameraPlaceable.height) / 2
                )
                val rightX = totalWidth - horizontalPaddingPx - rightPlaceable.width
                rightPlaceable.placeRelative(
                    x = rightX,
                    y = (totalHeight - rightPlaceable.height) / 2
                )
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "HyperOS Expanded Island", showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun HyperOsExpandedIslandPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        HyperOsExpandedIsland {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HyperOsLeftGraphic(
                    source = "DEFAULT",
                    highlightColor = Color(0xFF3482FF),
                    iconShape = RoundedCornerShape(12.dp)
                )
                HyperOsTextGroup(
                    title = "Order on the way",
                    subtitle = "Arriving in 15 mins",
                    highlightText = "In Transit",
                    highlightColor = Color(0xFF3482FF)
                )
            }
            HyperOsProgressBar(
                progressPercent = 70,
                progressSlot = ProgressSlotConfig(type = ProgressSlotType.WAYPOINT),
                highlightColor = Color(0xFF3482FF)
            )
            HyperOsActionButtons(
                actionSlots = listOf(
                    ActionSlotConfig(
                        slotPosition = 0,
                        isVisible = true,
                        displayMode = ActionDisplayMode.ICON_AND_TEXT,
                        customLabel = "Track"
                    ),
                    ActionSlotConfig(
                        slotPosition = 1,
                        isVisible = true,
                        displayMode = ActionDisplayMode.ICON_ONLY
                    )
                ),
                highlightColor = Color(0xFF3482FF)
            )
        }
    }
}

@Preview(name = "HyperOS Compact Pill (Timer)", showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun HyperOsCompactPillPreview() {
    Box(
        modifier = Modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        HyperOsCompactPill(
            leftDesign = PillLeftDesign.ICON_AND_TEXT,
            rightDesign = PillRightDesign.TIMER,
            title = "Timer",
            rightText = "04:30",
            highlightColor = Color(0xFF3482FF),
            iconShape = RoundedCornerShape(6.dp)
        )
    }
}

@Preview(name = "HyperOS Compact Pill (Circular Progress)", showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun HyperOsCompactPillProgressPreview() {
    Box(
        modifier = Modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        HyperOsCompactPill(
            leftDesign = PillLeftDesign.ICON_AND_TEXT,
            rightDesign = PillRightDesign.PROGRESS_PERCENT,
            title = "Downloading",
            progressPercent = 75,
            highlightColor = Color(0xFF22C55E),
            iconShape = RoundedCornerShape(6.dp)
        )
    }
}

@Preview(name = "Component: Left Graphic", showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun HyperOsLeftGraphicPreview() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HyperOsLeftGraphic(
            source = "APP_ICON",
            highlightColor = Color(0xFF3482FF),
            iconShape = RoundedCornerShape(12.dp)
        )
        HyperOsLeftGraphic(
            source = "AVATAR",
            highlightColor = Color(0xFF22C55E),
            iconShape = CircleShape
        )
    }
}

@Preview(name = "Component: Text Group", showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun HyperOsTextGroupPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        HyperOsTextGroup(
            title = "Flight LH 401",
            subtitle = "Boarding at Gate B22 • On time",
            highlightText = "Gate B22",
            highlightColor = Color(0xFFFF9800),
            textColor = Color.White
        )
    }
}

@Preview(name = "Component: Progress Bar & Waypoints", showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun HyperOsProgressBarPreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HyperOsProgressBar(
            progressPercent = 65,
            progressSlot = ProgressSlotConfig(type = ProgressSlotType.PROGRESS_BAR, showPercentage = true),
            highlightColor = Color(0xFF3482FF)
        )
        HyperOsProgressBar(
            progressPercent = 50,
            progressSlot = ProgressSlotConfig(type = ProgressSlotType.WAYPOINT),
            highlightColor = Color(0xFF22C55E)
        )
        HyperOsProgressBar(
            progressPercent = 0,
            progressSlot = ProgressSlotConfig(type = ProgressSlotType.TIMER),
            highlightColor = Color(0xFFFF9800)
        )
    }
}

@Preview(name = "Component: Action Buttons", showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun HyperOsActionButtonsPreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HyperOsActionButtons(
            actionSlots = listOf(
                ActionSlotConfig(
                    slotPosition = 0,
                    isVisible = true,
                    displayMode = ActionDisplayMode.ICON_AND_TEXT,
                    source = ActionSource.INLINE_REPLY
                ),
                ActionSlotConfig(
                    slotPosition = 1,
                    isVisible = true,
                    displayMode = ActionDisplayMode.ICON_ONLY,
                    source = ActionSource.SMART_ACTION,
                    smartActionType = SmartActionType.OTP_COPY
                )
            ),
            highlightColor = Color(0xFF3482FF)
        )
    }
}

