package com.d4viddf.hyperbridge.ui.screens.theme.content

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
import com.d4viddf.hyperbridge.ui.screens.theme.getShapeFromId

// --- SHARED PREVIEW CAROUSEL ---
@Composable
fun SharedThemePreview(
    highlightColorHex: String,
    useAppColors: Boolean,
    shapeId: String,
    paddingPercent: Int,
    answerColorHex: String,
    declineColorHex: String,
    answerShapeId: String,
    declineShapeId: String
) {
    val pagerState = rememberPagerState(pageCount = { 3 })

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 64.dp),
            pageSpacing = 16.dp
        ) { page ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                when (page) {
                    0 -> StandardIslandPreview(highlightColorHex, useAppColors)
                    1 -> ButtonIslandPreview(highlightColorHex, shapeId, paddingPercent)
                    2 -> CallIslandPreview(answerColorHex, declineColorHex, answerShapeId, declineShapeId, paddingPercent)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

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
                        .size(6.dp)
                )
            }
        }
    }
}

// --- SPECIFIC PREVIEWS (For Detail Screens) ---

@Composable
fun IconsSpecificPreview(
    highlightColorHex: String,
    shapeId: String,
    paddingPercent: Int
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(64.dp)
    ) {
        ButtonIslandPreview(highlightColorHex, shapeId, paddingPercent)
    }
}

@Composable
fun CallSpecificPreview(
    answerColorHex: String,
    declineColorHex: String,
    answerShapeId: String,
    declineShapeId: String,
    paddingPercent: Int
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(64.dp)
    ) {
        CallIslandPreview(answerColorHex, declineColorHex, answerShapeId, declineShapeId, paddingPercent)
    }
}

// --- INTERNAL IMPLEMENTATIONS ---

@Composable
private fun StandardIslandPreview(colorHex: String, useAppColors: Boolean) {
    val selectedColor = safeParseColor(colorHex)
    val appColor = if (useAppColors) Color(0xFF4CAF50) else selectedColor

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ButtonIslandPreview(colorHex: String, shapeId: String, paddingPercent: Int) {
    val selectedColor = safeParseColor(colorHex)
    val iconShape = getShapeFromId(shapeId).toShape()
    val padding = (32 * (paddingPercent / 100f)).dp

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CallIslandPreview(
    answerColorHex: String,
    declineColorHex: String,
    answerShapeId: String,
    declineShapeId: String,
    paddingPercent: Int
) {
    val answerColor = safeParseColor(answerColorHex)
    val declineColor = safeParseColor(declineColorHex)
    val answerShape = getShapeFromId(answerShapeId).toShape()
    val declineShape = getShapeFromId(declineShapeId).toShape()
    val padding = (32 * (paddingPercent / 100f)).dp

    IslandPill(width = 200.dp, height = 48.dp) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(Color.LightGray))
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(32.dp).background(declineColor.copy(alpha=0.2f), declineShape).padding(padding), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CallEnd, null, tint = declineColor)
                }
                Box(modifier = Modifier.size(32.dp).background(answerColor.copy(alpha=0.2f), answerShape).padding(padding), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Call, null, tint = answerColor)
                }
            }
        }
    }
}

@Composable
private fun IslandPill(width: Dp, height: Dp, borderColor: Color = Color(0xFF333333), content: @Composable () -> Unit) {
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