package com.d4viddf.hyperbridge.ui.screens.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.IslandTemplate
import com.d4viddf.hyperbridge.models.translator.IslandTemplateCatalog
import com.d4viddf.hyperbridge.ui.components.island.IslandTemplateGallery
import com.d4viddf.hyperbridge.ui.screens.translators.getTranslatorOutlinedIcon

/**
 * Adding a design: pick a template (or, once the Widget Studio lands, build a custom one), then
 * say when it should show. Nothing else -- a design starts as "this template, for this kind of
 * notification", and the translator editor is where match conditions and per-element bindings live.
 */
private enum class AddDesignStep { SOURCE, TEMPLATE, NOTIFICATION_TYPE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDesignFlow(
    onDismiss: () -> Unit,
    onDesignCreated: (CustomTranslator) -> Unit,
    onCustomDesign: (() -> Unit)? = null
) {
    var step by remember { mutableStateOf(AddDesignStep.SOURCE) }
    var chosenTemplate by remember { mutableStateOf<IslandTemplate?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        when (step) {
            AddDesignStep.SOURCE -> DesignSourceContent(
                customEnabled = onCustomDesign != null,
                onFromTemplate = { step = AddDesignStep.TEMPLATE },
                onCustom = { onCustomDesign?.invoke() }
            )

            AddDesignStep.TEMPLATE -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SheetHeader(
                    icon = Icons.Outlined.DashboardCustomize,
                    title = stringResource(R.string.design_template_gallery_title)
                )
                IslandTemplateGallery(
                    currentTemplateId = chosenTemplate?.id,
                    onTemplateSelected = { id ->
                        chosenTemplate = IslandTemplateCatalog.find(id)
                        step = AddDesignStep.NOTIFICATION_TYPE
                    },
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            // Only reachable with a template already chosen.
            AddDesignStep.NOTIFICATION_TYPE -> chosenTemplate?.let { template ->
                val designName = stringResource(template.nameRes)
                NotificationTypeContent(
                    template = template,
                    onTypeSelected = { type ->
                        onDesignCreated(IslandTemplateCatalog.newDesign(template, type, designName))
                    }
                )
            }
        }
    }
}

@Composable
private fun DesignSourceContent(
    customEnabled: Boolean,
    onFromTemplate: () -> Unit,
    onCustom: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SheetHeader(
            icon = Icons.Outlined.DashboardCustomize,
            title = stringResource(R.string.design_add_design_title)
        )

        DesignSourceCard(
            icon = Icons.Outlined.DashboardCustomize,
            title = stringResource(R.string.design_add_from_template),
            subtitle = stringResource(R.string.design_add_from_template_desc),
            enabled = true,
            onClick = onFromTemplate
        )

        DesignSourceCard(
            icon = Icons.Outlined.Widgets,
            title = stringResource(R.string.design_add_custom),
            subtitle = stringResource(R.string.design_add_custom_desc),
            enabled = customEnabled,
            onClick = onCustom
        )
    }
}
private fun getNotificationTypeIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.STANDARD -> Icons.Outlined.Notifications
        NotificationType.MESSAGE -> Icons.AutoMirrored.Outlined.Message
        NotificationType.PROGRESS -> Icons.Outlined.HourglassEmpty
        NotificationType.DOWNLOAD -> Icons.Outlined.CloudDownload
        NotificationType.MEDIA -> Icons.Outlined.MusicNote
        NotificationType.NAVIGATION -> Icons.Outlined.Map
        NotificationType.CALL -> Icons.Outlined.Call
        NotificationType.TIMER -> Icons.Outlined.Timer
        NotificationType.SCREEN_RECORDING -> Icons.Outlined.Videocam
    }
}

@Composable
private fun NotificationTypeContent(
    template: IslandTemplate,
    onTypeSelected: (NotificationType) -> Unit
) {
    val suggested = template.suggestedTypes
    val types = NotificationType.configurableEntries.sortedByDescending { suggested.contains(it) }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SheetHeader(
            icon = getTranslatorOutlinedIcon(template.iconName),
            title = stringResource(R.string.design_template_type_title)
        )
        Text(
            text = stringResource(R.string.design_template_type_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 2-column card grid
        val chunkedTypes = types.chunked(2)
        chunkedTypes.forEach { rowTypes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowTypes.forEach { type ->
                    val isSuggested = suggested.contains(type)
                    NotificationTypeGridCard(
                        type = type,
                        isSuggested = isSuggested,
                        modifier = Modifier.weight(1f),
                        onClick = { onTypeSelected(type) }
                    )
                }
                if (rowTypes.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun NotificationTypeGridCard(
    type: NotificationType,
    isSuggested: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val icon = getNotificationTypeIcon(type)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSuggested) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSuggested) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSuggested) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                if (isSuggested) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = stringResource(R.string.design_template_type_suggested),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = stringResource(type.labelRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SheetHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DesignSourceCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val contentAlpha = if (enabled) 1f else 0.4f
    Card(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = contentAlpha),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = contentAlpha),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                    )
                }
            }
        }
    }
}
