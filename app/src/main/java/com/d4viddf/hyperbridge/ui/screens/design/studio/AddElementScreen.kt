package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.widget.ButtonNode
import com.d4viddf.hyperbridge.models.widget.ContainerLayout
import com.d4viddf.hyperbridge.models.widget.CustomWidgetNode
import com.d4viddf.hyperbridge.models.widget.ImageNode
import com.d4viddf.hyperbridge.models.widget.LayoutContainer
import com.d4viddf.hyperbridge.models.widget.NodeBounds
import com.d4viddf.hyperbridge.models.widget.ProgressNode
import com.d4viddf.hyperbridge.models.widget.TextNode
import java.util.UUID

/** What the Studio's Add button offers, one screen of options (#328). */
enum class StudioElement(
    val labelRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector
) {
    TEXT(R.string.studio_add_text, R.string.studio_add_text_desc, Icons.Rounded.TextFields),
    IMAGE(R.string.studio_add_image, R.string.studio_add_image_desc, Icons.Rounded.Image),
    PROGRESS(R.string.studio_add_progress, R.string.studio_add_progress_desc, Icons.Rounded.Timeline),
    BUTTON(R.string.studio_add_button, R.string.studio_add_button_desc, Icons.Rounded.SmartButton),
    CONTAINER(R.string.studio_add_container, R.string.studio_add_container_desc, Icons.Rounded.Dashboard);

    fun create(): CustomWidgetNode {
        val id = UUID.randomUUID().toString().take(8)
        return when (this) {
            TEXT -> TextNode(id = id, template = "{notif.title}", bounds = NodeBounds(x = 8, y = 8))
            IMAGE -> ImageNode(id = id, bounds = NodeBounds(x = 8, y = 8, widthDp = 24, heightDp = 24))
            PROGRESS -> ProgressNode(id = id, bounds = NodeBounds(x = 8, y = 8, widthDp = 120, heightDp = 8))
            BUTTON -> ButtonNode(id = id, label = "Button", bounds = NodeBounds(x = 8, y = 8))
            CONTAINER -> LayoutContainer(id = id, layout = ContainerLayout.ROW, bounds = NodeBounds(x = 8, y = 8))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddElementScreen(
    onBack: () -> Unit,
    onPick: (StudioElement) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.studio_add_element_title)) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(StudioElement.entries) { element ->
                Card(
                    onClick = { onPick(element) },
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
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    element.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(element.labelRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(element.descriptionRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
