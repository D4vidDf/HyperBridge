package com.d4viddf.hyperbridge.ui.screens.design.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.d4viddf.hyperbridge.models.composer.ComposerTemplateType
import com.d4viddf.hyperbridge.models.composer.FieldBinding
import com.d4viddf.hyperbridge.models.composer.IslandTemplateDefinition
import com.d4viddf.hyperbridge.models.composer.NotificationField
import com.d4viddf.hyperbridge.models.composer.ProgressKind

/**
 * Mock notification data the live preview binds slot fields against. Field bindings are resolved
 * against this instead of a real [android.service.notification.StatusBarNotification] so the
 * preview works identically in the composer editor and in the static template catalog.
 */
data class PreviewSampleData(
    val title: String = "Sample App",
    val text: String = "Sample notification text",
    val subText: String = "Extra detail",
    val progressPercent: Int = 42
)

private fun resolve(binding: FieldBinding, sample: PreviewSampleData): String = when (binding.field) {
    NotificationField.TITLE -> sample.title
    NotificationField.TEXT -> sample.text
    NotificationField.SUBTEXT -> sample.subText
    NotificationField.PROGRESS -> "${sample.progressPercent}%"
    NotificationField.STATIC -> binding.staticValue ?: ""
    NotificationField.APP_ICON, NotificationField.LARGE_ICON -> ""
}

private fun safeColor(hex: String?, fallback: Color): Color = try {
    hex?.let { Color(it.toColorInt()) } ?: fallback
} catch (_: Exception) {
    fallback
}

/**
 * Shared live-preview pill for every Phase 4 template (#272), built on the same rounded-pill
 * scaffold as [com.d4viddf.hyperbridge.ui.components.PermanentIslandPreview]. Renders whichever
 * slots the current [definition] enables: left graphic, center text (+ optional badge), one
 * progress representation, and up to 3 action buttons.
 */
@Composable
fun IslandTemplatePreview(
    definition: IslandTemplateDefinition,
    sample: PreviewSampleData,
    modifier: Modifier = Modifier
) {
    val highlight = safeColor(definition.highlightColor, MaterialTheme.colorScheme.primary)
    val titleColor = safeColor(definition.text.titleColor, Color.White)
    val contentColor = safeColor(definition.text.contentColor, Color.White.copy(alpha = 0.7f))

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Left graphic slot
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(highlight.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Notifications, null, tint = highlight, modifier = Modifier.size(20.dp))
                }

                Spacer(Modifier.width(12.dp))

                // Center text slot
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            resolve(definition.text.title, sample),
                            color = titleColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (definition.text.showBadge) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(highlight)
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    definition.text.badgeText?.let { resolve(it, sample) } ?: "",
                                    color = Color.Black,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    Text(
                        resolve(definition.text.content, sample),
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                    definition.text.subContent?.let {
                        Text(
                            resolve(it, sample),
                            color = contentColor.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Progress slot (circular rendered beside the text, others rendered below)
                if (definition.progress.kind == ProgressKind.CIRCULAR) {
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(
                        progress = { resolveProgress(definition, sample) },
                        modifier = Modifier.size(28.dp),
                        color = safeColor(definition.progress.activeColor, highlight),
                        strokeWidth = 3.dp
                    )
                }
            }

            when (definition.progress.kind) {
                ProgressKind.LINEAR -> {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { resolveProgress(definition, sample) },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = safeColor(definition.progress.activeColor, highlight)
                    )
                }
                ProgressKind.MULTI_STEP -> {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                        val steps = 4
                        val filled = (resolveProgress(definition, sample) * steps).toInt().coerceIn(0, steps)
                        repeat(steps) { index ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (index < filled) safeColor(definition.progress.activeColor, highlight)
                                        else Color.White.copy(alpha = 0.2f)
                                    )
                            )
                        }
                    }
                }
                else -> {}
            }

            if (definition.buttons.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    definition.buttons.take(3).forEach { button ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(safeColor(button.bgColor, highlight.copy(alpha = 0.3f)))
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                button.label.ifBlank { "Button" },
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun resolveProgress(definition: IslandTemplateDefinition, sample: PreviewSampleData): Float {
    val binding = definition.progress.valueBinding
    val value = if (binding.field == NotificationField.STATIC) {
        binding.staticValue?.toIntOrNull() ?: 0
    } else {
        sample.progressPercent
    }
    return (value.coerceIn(0, 100) / 100f)
}

/** Default slot layouts for each of the 10 Xiaomi templates, used by the catalog previews. */
fun defaultDefinitionFor(type: ComposerTemplateType): IslandTemplateDefinition = IslandTemplateDefinition(
    templateType = type,
    progress = when (type) {
        ComposerTemplateType.T4_RIDE_WAYPOINTS, ComposerTemplateType.T6_PARKING_CHARGING ->
            com.d4viddf.hyperbridge.models.composer.ProgressSlotConfig(kind = ProgressKind.LINEAR)
        ComposerTemplateType.T5_QUEUE, ComposerTemplateType.T7_TRANSFER ->
            com.d4viddf.hyperbridge.models.composer.ProgressSlotConfig(kind = ProgressKind.CIRCULAR)
        else -> com.d4viddf.hyperbridge.models.composer.ProgressSlotConfig(kind = ProgressKind.NONE)
    },
    buttons = when (type) {
        ComposerTemplateType.T8_COUPON -> listOf(com.d4viddf.hyperbridge.models.composer.ComposerActionButton(label = "Use now"))
        ComposerTemplateType.T9_BOARDING_PASS -> listOf(com.d4viddf.hyperbridge.models.composer.ComposerActionButton(label = "View pass"))
        ComposerTemplateType.T10_COURIER -> listOf(com.d4viddf.hyperbridge.models.composer.ComposerActionButton(label = "Track"))
        else -> emptyList()
    }
)

private val CATALOG_SAMPLE = PreviewSampleData(
    title = "Preview",
    text = "This is what the island looks like",
    subText = "Extra line",
    progressPercent = 60
)

@Composable fun Template1Preview(modifier: Modifier = Modifier) = IslandTemplatePreview(defaultDefinitionFor(ComposerTemplateType.T1_WEATHER_NAV), CATALOG_SAMPLE, modifier)
@Composable fun Template2Preview(modifier: Modifier = Modifier) = IslandTemplatePreview(defaultDefinitionFor(ComposerTemplateType.T2_PAYMENT_OTP), CATALOG_SAMPLE, modifier)
@Composable fun Template3Preview(modifier: Modifier = Modifier) = IslandTemplatePreview(defaultDefinitionFor(ComposerTemplateType.T3_CALL_MEETING), CATALOG_SAMPLE, modifier)
@Composable fun Template4Preview(modifier: Modifier = Modifier) = IslandTemplatePreview(defaultDefinitionFor(ComposerTemplateType.T4_RIDE_WAYPOINTS), CATALOG_SAMPLE, modifier)
@Composable fun Template5Preview(modifier: Modifier = Modifier) = IslandTemplatePreview(defaultDefinitionFor(ComposerTemplateType.T5_QUEUE), CATALOG_SAMPLE, modifier)
@Composable fun Template6Preview(modifier: Modifier = Modifier) = IslandTemplatePreview(defaultDefinitionFor(ComposerTemplateType.T6_PARKING_CHARGING), CATALOG_SAMPLE, modifier)
@Composable fun Template7Preview(modifier: Modifier = Modifier) = IslandTemplatePreview(defaultDefinitionFor(ComposerTemplateType.T7_TRANSFER), CATALOG_SAMPLE, modifier)
@Composable fun Template8Preview(modifier: Modifier = Modifier) = IslandTemplatePreview(defaultDefinitionFor(ComposerTemplateType.T8_COUPON), CATALOG_SAMPLE, modifier)
@Composable fun Template9Preview(modifier: Modifier = Modifier) = IslandTemplatePreview(defaultDefinitionFor(ComposerTemplateType.T9_BOARDING_PASS), CATALOG_SAMPLE, modifier)
@Composable fun Template10Preview(modifier: Modifier = Modifier) = IslandTemplatePreview(defaultDefinitionFor(ComposerTemplateType.T10_COURIER), CATALOG_SAMPLE, modifier)
