package com.d4viddf.hyperbridge.ui.components.island

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.translator.IslandTemplate
import com.d4viddf.hyperbridge.models.translator.IslandTemplateCatalog
import com.d4viddf.hyperbridge.ui.screens.translators.getTranslatorOutlinedIcon

/**
 * The ten official island templates, each previewed by the same [HyperOsIslandPreview] the
 * translator editor uses, so what the gallery shows is what the island will look like.
 */
@Composable
fun IslandTemplateGallery(
    currentTemplateId: String?,
    onTemplateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(IslandTemplateCatalog.gallery, key = { it.id }) { template ->
            IslandTemplateCard(
                template = template,
                isSelected = template.id == currentTemplateId,
                onClick = { onTemplateSelected(template.id) }
            )
        }
    }
}

@Composable
fun IslandTemplateCard(
    template: IslandTemplate,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sample = rememberIslandTemplateSample(template)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getTranslatorOutlinedIcon(template.iconName),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(template.nameRes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(template.descriptionRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isSelected) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HyperOsIslandPreview(
                translator = template.previewTranslator(),
                sample = sample,
                showChrome = false
            )

            if (template.showsProgress || template.showsActions) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (template.showsProgress) {
                        TemplateBadge(stringResource(R.string.island_template_badge_progress))
                    }
                    if (template.showsActions) {
                        TemplateBadge(stringResource(R.string.island_template_badge_actions))
                    }
                }
            }
        }
    }
}

/** The example notification a template previews against. */
@Composable
fun rememberIslandTemplateSample(template: IslandTemplate): IslandPreviewSample {
    val title = stringResource(template.sampleTitleRes)
    val text = stringResource(template.sampleTextRes)
    val highlight = template.sampleHighlightRes?.let { stringResource(it) }
    return remember(title, text, highlight) {
        IslandPreviewSample(
            title = title,
            text = text,
            subtext = highlight ?: text,
            track = title,
            artist = text
        )
    }
}

@Composable
private fun TemplateBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Preview(name = "Island Template Gallery", showBackground = true)
@Composable
private fun IslandTemplateGalleryPreview() {
    MaterialTheme {
        Surface {
            IslandTemplateCard(
                template = IslandTemplateCatalog.gallery.first(),
                isSelected = true,
                onClick = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
