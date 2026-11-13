package com.d4viddf.hyperbridge.ui.screens.translators

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SportsScore
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.ui.graphics.vector.ImageVector

data class TranslatorIconOption(
    val id: String,
    val label: String,
    val icon: ImageVector
)

val TRANSLATOR_OUTLINED_ICONS = listOf(
    TranslatorIconOption("AutoAwesome", "Magic", Icons.Outlined.AutoAwesome),
    TranslatorIconOption("MusicNote", "Music", Icons.Outlined.MusicNote),
    TranslatorIconOption("Navigation", "Navigation", Icons.Outlined.Navigation),
    TranslatorIconOption("DirectionsCar", "Rideshare", Icons.Outlined.DirectionsCar),
    TranslatorIconOption("LocalShipping", "Delivery", Icons.Outlined.LocalShipping),
    TranslatorIconOption("Chat", "Chat", Icons.AutoMirrored.Outlined.Chat),
    TranslatorIconOption("Timer", "Timer", Icons.Outlined.Timer),
    TranslatorIconOption("Download", "Download", Icons.Outlined.Download),
    TranslatorIconOption("SportsScore", "Sports", Icons.Outlined.SportsScore),
    TranslatorIconOption("Notifications", "Alerts", Icons.Outlined.Notifications),
    TranslatorIconOption("Call", "Phone", Icons.Outlined.Call),
    TranslatorIconOption("Flight", "Flights", Icons.Outlined.Flight),
    TranslatorIconOption("Train", "Transit", Icons.Outlined.Train),
    TranslatorIconOption("ShoppingBag", "Shopping", Icons.Outlined.ShoppingBag),
    TranslatorIconOption("Favorite", "Health", Icons.Outlined.Favorite),
    TranslatorIconOption("Star", "Star", Icons.Outlined.Star),
    TranslatorIconOption("Lightbulb", "Utilities", Icons.Outlined.Lightbulb),
    TranslatorIconOption("Code", "Developer", Icons.Outlined.Code),
    TranslatorIconOption("Extension", "Extension", Icons.Outlined.Extension),
    TranslatorIconOption("Widgets", "Widgets", Icons.Outlined.Widgets),
    TranslatorIconOption("Speed", "Performance", Icons.Outlined.Speed),
    TranslatorIconOption("Apps", "Apps", Icons.Outlined.Apps),
    TranslatorIconOption("Public", "Global", Icons.Outlined.Public),
    TranslatorIconOption("Category", "Category", Icons.Outlined.Category)
)

fun getTranslatorOutlinedIcon(iconName: String?): ImageVector {
    return TRANSLATOR_OUTLINED_ICONS.firstOrNull { it.id.equals(iconName, ignoreCase = true) }?.icon
        ?: Icons.Outlined.AutoAwesome
}
