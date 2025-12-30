package com.d4viddf.hyperbridge.ui.screens.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// --- 1. MAIN CAROUSEL (For General Screen) ---
@Composable
fun ThemeCarouselPreview(viewModel: ThemeViewModel) {
    val pagerState = rememberPagerState(pageCount = { 3 })

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp) // Reduced vertical padding
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 64.dp), // Wider peek to see neighbors
            pageSpacing = 16.dp
        ) { page ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp) // Fixed lower height for the preview area
            ) {
                when (page) {
                    0 -> StandardIslandPreview(viewModel)
                    1 -> ButtonIslandPreview(viewModel)
                    2 -> CallIslandPreview(viewModel)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Indicators
        Row(
            Modifier.wrapContentHeight().fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(6.dp) // Smaller dots
                )
            }
        }
    }
}

// --- 2. SPECIFIC PREVIEWS (For Detail Screens) ---

@Composable
fun IconsSpecificPreview(viewModel: ThemeViewModel) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(64.dp)
    ) {
        ButtonIslandPreview(viewModel)
    }
}

@Composable
fun CallSpecificPreview(viewModel: ThemeViewModel) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(64.dp)
    ) {
        CallIslandPreview(viewModel)
    }
}

// --- INTERNAL IMPLEMENTATIONS ---

// Preview 1: Standard (Text + Accent)
@Composable
private fun StandardIslandPreview(viewModel: ThemeViewModel) {
    val selectedColor = safeParseColor(viewModel.selectedColorHex)
    val appColor = if (viewModel.useAppColors) Color(0xFF4CAF50) else selectedColor

    IslandPill(width = 180.dp, height = 48.dp) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(appColor.copy(alpha=0.2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Notifications, null, tint = appColor, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Box(Modifier.width(60.dp).height(6.dp).background(selectedColor, CircleShape))
                Spacer(Modifier.height(4.dp))
                Box(Modifier.width(40.dp).height(6.dp).background(Color.Gray, CircleShape))
            }
        }
    }
}

// Preview 2: Buttons (Shape Showcase)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ButtonIslandPreview(viewModel: ThemeViewModel) {
    val selectedColor = safeParseColor(viewModel.selectedColorHex)
    val iconShape = getShapeFromId(viewModel.selectedShapeId).toShape()
    val padding = (32 * (viewModel.iconPaddingPercent / 100f)).dp

    IslandPill(width = 220.dp, height = 48.dp) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(Color.DarkGray))
            Spacer(Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(32.dp).background(selectedColor.copy(alpha = 0.2f), iconShape).padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Call, null, tint = selectedColor)
                }
                Box(
                    modifier = Modifier.size(32.dp).background(selectedColor.copy(alpha = 0.2f), iconShape).padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Mic, null, tint = selectedColor)
                }
            }
        }
    }
}

// Preview 3: Calls (Answer/Decline)
@Composable
private fun CallIslandPreview(viewModel: ThemeViewModel) {
    val answerColor = safeParseColor(viewModel.callAnswerColor)
    val declineColor = safeParseColor(viewModel.callDeclineColor)

    IslandPill(width = 200.dp, height = 48.dp) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Caller Icon
            Box(Modifier.size(32.dp).clip(CircleShape).background(Color.LightGray))
            Spacer(Modifier.weight(1f))

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Decline
                Box(Modifier.size(32.dp).background(declineColor.copy(alpha=0.2f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CallEnd, null, tint = declineColor, modifier = Modifier.size(18.dp))
                }
                // Answer
                Box(Modifier.size(32.dp).background(answerColor.copy(alpha=0.2f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Call, null, tint = answerColor, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun IslandPill(
    width: Dp,
    height: Dp,
    borderColor: Color = Color(0xFF333333),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.size(width, height),
        shape = RoundedCornerShape(height / 2),
        color = Color.Black,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 8.dp
    ) {
        content()
    }
}