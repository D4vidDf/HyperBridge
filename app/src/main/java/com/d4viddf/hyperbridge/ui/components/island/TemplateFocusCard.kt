package com.d4viddf.hyperbridge.ui.components.island

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d4viddf.hyperbridge.models.translator.IslandTemplateCatalog
import com.d4viddf.hyperbridge.models.translator.TemplateCardLayout
import com.d4viddf.hyperbridge.models.translator.TemplateCardLayout.Hint
import com.d4viddf.hyperbridge.models.translator.TemplateCardLayout.Progress
import com.d4viddf.hyperbridge.models.translator.TemplateCardLayout.Style
import com.d4viddf.hyperbridge.ui.screens.translators.getTranslatorOutlinedIcon

private val CardColor = Color(0xFF232323)
private val TrackColor = Color(0xFF3A3A3A)
private val SecondaryText = Color(0xFF9E9E9E)

/**
 * A template as HyperOS draws it: the focus notification card that floats from the island and
 * sits in the shade. Laid out from the HyperIsland ToolKit demo's templates on a HyperOS 3 phone,
 * with the design's own text bound into Xiaomi's title / content / hint fields.
 */
@Composable
fun TemplateFocusCard(
    layout: TemplateCardLayout,
    iconName: String,
    title: String,
    subtitle: String,
    highlight: String?,
    accent: Color,
    progressPercent: Int,
    modifier: Modifier = Modifier
) {
    val extra = highlight?.takeIf { it.isNotBlank() }
    // With a hint row the highlight lives there, not repeated on the title line.
    val titleExtra = if (layout.hint == Hint.NONE) extra else null
    val glyph = getTranslatorOutlinedIcon(iconName)

    val background = if (layout.tintedBackground) {
        Brush.horizontalGradient(listOf(Color(0xFF2A100C), accent.copy(alpha = 0.9f)))
    } else {
        Brush.linearGradient(listOf(CardColor, CardColor))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        when (layout.style) {
            Style.BASE_TYPE_1 -> Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    CardText(subtitle, SecondaryText, 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CardText(title, accent, 17.sp, FontWeight.SemiBold, Modifier.weight(1f, fill = false))
                        if (titleExtra != null) {
                            Spacer(Modifier.width(8.dp))
                            CardText(titleExtra, Color.White, 17.sp, FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Icon(glyph, null, tint = Color.White, modifier = Modifier.size(34.dp))
            }

            Style.BASE_TYPE_2 -> Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CardText(title, Color.White, 17.sp, FontWeight.SemiBold, Modifier.weight(1f, fill = false))
                        if (titleExtra != null) {
                            Spacer(Modifier.width(8.dp))
                            CardText(titleExtra, Color.White, 17.sp, FontWeight.SemiBold)
                        }
                    }
                    CardText(subtitle, SecondaryText, 13.sp)
                }
                if (layout.progress != Progress.VEHICLE && !layout.tintedBackground) {
                    Spacer(Modifier.width(12.dp))
                    AppPicture(glyph, accent)
                }
            }

            Style.CHAT -> Row(verticalAlignment = Alignment.CenterVertically) {
                Thumbnail(glyph)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    CardText(title, Color.White, 16.sp, FontWeight.SemiBold)
                    CardText(subtitle, SecondaryText, 13.sp)
                }
                if (layout.hint == Hint.NONE) {
                    Spacer(Modifier.width(12.dp))
                    Icon(glyph, null, tint = accent, modifier = Modifier.size(24.dp))
                }
            }

            Style.CALL -> Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = TrackColor, modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    CardText(title, Color.White, 16.sp, FontWeight.SemiBold)
                    CardText(subtitle, SecondaryText, 13.sp)
                }
                RoundButton(Icons.Filled.CallEnd, Color(0xFFFF3B30))
                Spacer(Modifier.width(10.dp))
                RoundButton(Icons.Filled.Call, Color(0xFF34C759))
            }
        }

        when (layout.progress) {
            Progress.NONE -> Unit
            Progress.BAR -> {
                Spacer(Modifier.height(16.dp))
                PlainBar(progressPercent, accent)
            }
            Progress.VEHICLE -> {
                Spacer(Modifier.height(10.dp))
                VehicleBar(progressPercent, accent)
            }
        }

        when (layout.hint) {
            Hint.NONE -> Unit
            Hint.BUTTON -> {
                HintDivider(layout.tintedBackground)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CardText(extra ?: title, Color(0xFFBDBDBD), 16.sp, modifier = Modifier.weight(1f))
                    layout.hintButtonRes?.let { res ->
                        Surface(shape = RoundedCornerShape(50), color = TrackColor) {
                            Text(
                                text = stringResource(res),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
            Hint.TIMER -> {
                HintDivider(layout.tintedBackground)
                val (label, value) = splitHint(extra ?: subtitle)
                if (label.isNotEmpty()) CardText(label, Color.White.copy(alpha = 0.7f), 11.sp)
                CardText(value, Color.White, 16.sp, FontWeight.Bold)
            }
        }
    }
}

/** "Boarding 18:40" -> ("Boarding", "18:40"): the ticket hint shows a small label over the time. */
private fun splitHint(text: String): Pair<String, String> {
    val cut = text.trim().lastIndexOf(' ')
    return if (cut <= 0) "" to text.trim() else text.substring(0, cut).trim() to text.substring(cut + 1).trim()
}

@Composable
private fun CardText(
    text: String,
    color: Color,
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = color,
        fontSize = size,
        fontWeight = weight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun AppPicture(glyph: ImageVector, accent: Color) {
    Surface(shape = RoundedCornerShape(9.dp), color = accent, modifier = Modifier.size(30.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(glyph, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun Thumbnail(glyph: ImageVector) {
    Surface(shape = RoundedCornerShape(10.dp), color = TrackColor, modifier = Modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(glyph, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun RoundButton(icon: ImageVector, color: Color) {
    Surface(shape = CircleShape, color = color, modifier = Modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun HintDivider(onTint: Boolean) {
    HorizontalDivider(
        color = if (onTint) Color.White.copy(alpha = 0.2f) else TrackColor,
        modifier = Modifier.padding(vertical = 10.dp)
    )
}

@Composable
private fun PlainBar(progressPercent: Int, accent: Color) {
    val fraction = (progressPercent.coerceIn(0, 100) / 100f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(TrackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(accent)
        )
    }
}

/** Template 4's bar: solid up to the vehicle, dashed to the destination flag. */
@Composable
private fun VehicleBar(progressPercent: Int, accent: Color) {
    val fraction = (progressPercent.coerceIn(0, 100) / 100f)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(30.dp)) {
        val vehicleSize = 26.dp
        val flagSize = 16.dp
        Canvas(modifier = Modifier.fillMaxWidth().height(30.dp)) {
            val y = size.height - 4.dp.toPx()
            val split = size.width * fraction
            drawLine(
                brush = Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.7f)), endX = split),
                start = Offset(0f, y),
                end = Offset(split, y),
                strokeWidth = 7.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = TrackColor,
                start = Offset(split, y),
                end = Offset(size.width, y),
                strokeWidth = 7.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF6E6E6E),
                start = Offset(split + 8.dp.toPx(), y),
                end = Offset(size.width - flagSize.toPx(), y),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
            )
        }
        Icon(
            Icons.Filled.LocalTaxi,
            null,
            tint = Color(0xFFFFC928),
            modifier = Modifier
                .size(vehicleSize)
                .offset(x = (maxWidth - vehicleSize) * fraction)
        )
        Icon(
            Icons.Filled.Flag,
            null,
            tint = Color(0xFF9E9E9E),
            modifier = Modifier
                .size(flagSize)
                .align(Alignment.TopEnd)
                .offset(y = 4.dp)
        )
    }
}

@Preview(name = "Template focus cards", showBackground = true, backgroundColor = 0xFF000000, widthDp = 380)
@Composable
private fun TemplateFocusCardPreview() {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        IslandTemplateCatalog.gallery.forEach { template ->
            val card = template.card ?: return@forEach
            TemplateFocusCard(
                layout = card,
                iconName = template.iconName,
                title = template.id,
                subtitle = "Content line",
                highlight = "Hint 12:00",
                accent = Color(card.accentArgb),
                progressPercent = 45
            )
        }
    }
}
