package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.widget.ButtonNode
import com.d4viddf.hyperbridge.models.widget.CustomWidgetNode
import com.d4viddf.hyperbridge.models.widget.LayoutContainer
import com.d4viddf.hyperbridge.models.widget.TextNode

enum class StudioTab(val labelRes: Int) {
    ITEMS(R.string.studio_tab_items),
    ITEM(R.string.studio_tab_item),
    POSITION(R.string.studio_tab_position),
    COLORS(R.string.studio_tab_colors),
    EFX(R.string.studio_tab_efx),
    VALUE(R.string.studio_tab_value),
    ACTIONS(R.string.studio_tab_actions),
    CONTAINER(R.string.studio_tab_container),
    BACKGROUND(R.string.studio_tab_background),
    GLOBAL(R.string.studio_tab_global),
    DESIGN(R.string.studio_tab_design),
    SCOPE(R.string.studio_tab_scope),
    LAYER(R.string.studio_tab_layer),
    BINDINGS(R.string.studio_tab_bindings);

    companion object {
        fun tabsFor(node: CustomWidgetNode?, isRoot: Boolean): List<StudioTab> {
            return when {
                node == null || isRoot -> listOf(ITEMS, BACKGROUND, GLOBAL, DESIGN, SCOPE)
                node is TextNode -> listOf(ITEM, COLORS, EFX, POSITION)
                node is LayoutContainer -> listOf(ITEMS, ITEM, POSITION, CONTAINER, COLORS)
                else -> listOf(ITEM, POSITION, COLORS, VALUE, ACTIONS)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StudioTabsRow(
    tabs: List<StudioTab>,
    selectedTab: StudioTab,
    onTabSelected: (StudioTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 16.dp,
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                text = { Text(stringResource(tab.labelRes)) }
            )
        }
    }
}
