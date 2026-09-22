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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DashboardCustomize
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
                    modifier = Modifier.heightIn(max = 520.dp)
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

@Composable
private fun NotificationTypeContent(
    template: IslandTemplate,
    onTypeSelected: (NotificationType) -> Unit
) {
    val suggested = template.suggestedTypes
    val types = NotificationType.configurableEntries.sortedByDescending { suggested.contains(it) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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

        types.forEach { type ->
            DesignSourceCard(
                icon = getTranslatorOutlinedIcon(template.iconName),
                title = stringResource(type.labelRes),
                subtitle = if (suggested.contains(type)) {
                    stringResource(template.descriptionRes)
                } else {
                    ""
                },
                enabled = true,
                onClick = { onTypeSelected(type) }
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
