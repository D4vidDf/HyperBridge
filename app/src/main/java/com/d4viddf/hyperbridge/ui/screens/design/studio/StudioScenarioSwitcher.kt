package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.widget.VariableContext

enum class StudioPreviewScenario(val labelRes: Int, val icon: ImageVector) {
    STANDARD(R.string.studio_scenario_standard, Icons.Rounded.Notifications),
    MESSAGE(R.string.studio_scenario_message, Icons.Rounded.Email),
    PROGRESS(R.string.studio_scenario_progress, Icons.Rounded.LinearScale),
    OTP(R.string.studio_scenario_otp, Icons.Rounded.SmartButton),
    MEDIA(R.string.studio_scenario_media, Icons.Rounded.PlayArrow);

    fun toVariableContext(): VariableContext = when (this) {
        STANDARD -> VariableContext(
            notifTitle = "Calendar Event",
            notifText = "Design Review in 15 mins",
            notifProgress = null,
            notifPackage = "com.google.android.calendar",
            deviceBatteryPercent = 85,
            timeNowFormatted = "10:30",
            notificationActionTitles = listOf("Open", "Dismiss"),
            hasInlineReply = false,
            smartActionTypes = emptySet()
        )
        MESSAGE -> VariableContext(
            notifTitle = "Elena Rostova",
            notifText = "Hey! Did you check the new island layout?",
            notifProgress = null,
            notifPackage = "org.telegram.messenger",
            deviceBatteryPercent = 78,
            timeNowFormatted = "11:42",
            notificationActionTitles = listOf("Mark as read", "Reply"),
            hasInlineReply = true,
            smartActionTypes = setOf("URL")
        )
        PROGRESS -> VariableContext(
            notifTitle = "Downloading update...",
            notifText = "HyperOS 2.0 Patch (64%)",
            notifProgress = 64,
            notifPackage = "com.android.providers.downloads",
            deviceBatteryPercent = 52,
            timeNowFormatted = "14:15",
            notificationActionTitles = listOf("Pause", "Cancel"),
            hasInlineReply = false,
            smartActionTypes = emptySet()
        )
        OTP -> VariableContext(
            notifTitle = "Bank Security Code",
            notifText = "Your verification code is 849201. Valid for 5 min.",
            notifProgress = null,
            notifPackage = "com.bank.app",
            deviceBatteryPercent = 90,
            timeNowFormatted = "16:05",
            notificationActionTitles = listOf("Copy code"),
            hasInlineReply = false,
            smartActionTypes = setOf("OTP")
        )
        MEDIA -> VariableContext(
            notifTitle = "Midnight City",
            notifText = "M83 • Hurry Up, We're Dreaming",
            notifProgress = 45,
            notifPackage = "com.spotify.music",
            deviceBatteryPercent = 65,
            timeNowFormatted = "21:30",
            notificationActionTitles = listOf("Previous", "Play", "Next"),
            hasInlineReply = false,
            smartActionTypes = emptySet()
        )
    }
}

/**
 * Expressive scenario switcher allowing the creator to immediately test notification
 * variables and condition evaluations ({notif.progress}, OTP smart action, inline reply, etc.)
 */
@Composable
fun StudioScenarioSwitcher(
    selectedScenario: StudioPreviewScenario,
    onScenarioSelected: (StudioPreviewScenario) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudioPreviewScenario.entries.forEach { scenario ->
            val isSelected = scenario == selectedScenario
            FilterChip(
                selected = isSelected,
                onClick = { onScenarioSelected(scenario) },
                label = { Text(stringResource(scenario.labelRes), style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(
                        imageVector = scenario.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}
