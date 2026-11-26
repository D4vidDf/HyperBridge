package com.d4viddf.hyperbridge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.NavContent


import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity

@Composable
fun NavPreview(left: NavContent, right: NavContent) {
    val leftLabel = stringResource(getNavContentLabelRes(left))
    val rightLabel = stringResource(getNavContentLabelRes(right))
    val cd = stringResource(R.string.cd_nav_preview, leftLabel, rightLabel)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = cd }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
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
                SymmetricalNavIslandLayout(
                    left = left,
                    right = right
                )
            }
        }
    }
}

@Composable
private fun SymmetricalNavIslandLayout(
    left: NavContent,
    right: NavContent,
    modifier: Modifier = Modifier
) {
    val horizontalPaddingPx = with(LocalDensity.current) { 16.dp.roundToPx() }
    val cameraGapPx = with(LocalDensity.current) { 10.dp.roundToPx() }
    val minSideWidthPx = with(LocalDensity.current) { 16.dp.roundToPx() }
    val pillHeightPx = with(LocalDensity.current) { 44.dp.roundToPx() }

    Layout(
        modifier = modifier,
        content = {
            // Measurable 0: Left Content
            Box(contentAlignment = Alignment.CenterStart) {
                if (left != NavContent.NONE) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Default.TurnRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        NavContentRenderer(left, Alignment.Start)
                    }
                }
            }

            // Measurable 1: Camera Cutout (Realistic punch-hole)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F0F0F))
                )
            }

            // Measurable 2: Right Content
            Box(contentAlignment = Alignment.CenterEnd) {
                NavContentRenderer(right, Alignment.End)
            }
        }
    ) { measurables, constraints ->
        val unconstrained = constraints.copy(minWidth = 0, minHeight = 0)
        val leftPlaceable = measurables[0].measure(unconstrained)
        val cameraPlaceable = measurables[1].measure(unconstrained)
        val rightPlaceable = measurables[2].measure(unconstrained)

        // Make left and right symmetrical by taking the width of the widest side
        val sideWidth = maxOf(leftPlaceable.width, rightPlaceable.width, minSideWidthPx)

        val totalWidth = (horizontalPaddingPx * 2) + (sideWidth * 2) + cameraPlaceable.width + (cameraGapPx * 2)
        val totalHeight = pillHeightPx

        layout(totalWidth, totalHeight) {
            // Left content aligned at start of left side
            leftPlaceable.placeRelative(
                x = horizontalPaddingPx,
                y = (totalHeight - leftPlaceable.height) / 2
            )

            // Camera placed in the exact center
            val cameraX = horizontalPaddingPx + sideWidth + cameraGapPx
            cameraPlaceable.placeRelative(
                x = cameraX,
                y = (totalHeight - cameraPlaceable.height) / 2
            )

            // Right content aligned at end of right side
            val rightX = totalWidth - horizontalPaddingPx - rightPlaceable.width
            rightPlaceable.placeRelative(
                x = rightX,
                y = (totalHeight - rightPlaceable.height) / 2
            )
        }
    }
}

@Composable
fun NavContentRenderer(type: NavContent, align: Alignment.Horizontal) {

    // Fade Logic: Only applies to long text (Instruction)
    val fadeBrush = if (type == NavContent.INSTRUCTION) {
        if (align == Alignment.Start) {
            Brush.horizontalGradient(0.85f to Color.White, 1.0f to Color.Transparent)
        } else {
            Brush.horizontalGradient(0.0f to Color.Transparent, 0.15f to Color.White)
        }
    } else null

    val textStyle = if (fadeBrush != null) {
        TextStyle(brush = fadeBrush, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    } else {
        TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }

    val timeStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Normal, fontSize = 14.sp)

    when (type) {
        NavContent.INSTRUCTION -> {
            Text(
                text = stringResource(R.string.nav_preview_instruction),
                style = textStyle,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
        NavContent.DISTANCE -> {
            Text(
                text = stringResource(R.string.nav_preview_distance),
                style = textStyle // Solid White
            )
        }
        NavContent.ETA -> {
            Text(
                text = stringResource(R.string.nav_preview_time),
                style = timeStyle // Standard weight
            )
        }
        NavContent.DISTANCE_ETA -> {
            // Combined Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.nav_preview_distance), style = textStyle.copy(fontSize = 13.sp))
                Spacer(Modifier.width(4.dp))
                Text("•", color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.nav_preview_time), style = timeStyle.copy(fontSize = 13.sp))
            }
        }
        NavContent.NONE -> { /* Empty */ }
    }
}

fun getNavContentLabelRes(content: NavContent): Int {
    return when(content) {
        NavContent.INSTRUCTION -> R.string.nav_content_instruction
        NavContent.DISTANCE -> R.string.nav_content_distance
        NavContent.ETA -> R.string.nav_content_eta
        NavContent.DISTANCE_ETA -> R.string.nav_content_distance_eta
        NavContent.NONE -> R.string.nav_content_none
    }
}