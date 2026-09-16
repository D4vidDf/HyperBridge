package com.d4viddf.hyperbridge.models.composer

import androidx.annotation.StringRes
import com.d4viddf.hyperbridge.R
import kotlinx.serialization.Serializable

/**
 * The 10 official Xiaomi Super Island templates (issue #272). Each entry names the exact
 * combination of slots the template shows; [supportsProgress]/[supportsButtons] gate which slot
 * editors appear in the composer for that template.
 */
enum class ComposerTemplateType(
    @StringRes val labelRes: Int,
    val supportsProgress: Boolean,
    val supportsButtons: Boolean
) {
    T1_WEATHER_NAV(R.string.template_type_t1, supportsProgress = false, supportsButtons = false),
    T2_PAYMENT_OTP(R.string.template_type_t2, supportsProgress = false, supportsButtons = false),
    T3_CALL_MEETING(R.string.template_type_t3, supportsProgress = false, supportsButtons = false),
    T4_RIDE_WAYPOINTS(R.string.template_type_t4, supportsProgress = true, supportsButtons = false),
    T5_QUEUE(R.string.template_type_t5, supportsProgress = true, supportsButtons = false),
    T6_PARKING_CHARGING(R.string.template_type_t6, supportsProgress = true, supportsButtons = false),
    T7_TRANSFER(R.string.template_type_t7, supportsProgress = true, supportsButtons = false),
    T8_COUPON(R.string.template_type_t8, supportsProgress = false, supportsButtons = true),
    T9_BOARDING_PASS(R.string.template_type_t9, supportsProgress = false, supportsButtons = true),
    T10_COURIER(R.string.template_type_t10, supportsProgress = false, supportsButtons = true)
}

/** Notification fields a slot can be bound to, matching the issue's {title}/{text}/... tokens. */
enum class NotificationField {
    TITLE, TEXT, SUBTEXT, PROGRESS, APP_ICON, LARGE_ICON, STATIC
}

@Serializable
data class FieldBinding(
    val field: NotificationField = NotificationField.TEXT,
    val staticValue: String? = null
)

enum class GraphicSource { APP_ICON, LARGE_ICON, STATIC_ASSET }

@Serializable
data class LeftGraphicSlot(
    val source: GraphicSource = GraphicSource.APP_ICON,
    val shapeId: String = "circle",
    val paddingPercent: Int = 15
)

@Serializable
data class TextSlotConfig(
    val title: FieldBinding = FieldBinding(NotificationField.TITLE),
    val content: FieldBinding = FieldBinding(NotificationField.TEXT),
    val subContent: FieldBinding? = null,
    val titleColor: String? = null,
    val contentColor: String? = null,
    val showBadge: Boolean = false,
    val badgeText: FieldBinding? = null
)

enum class ProgressKind { NONE, LINEAR, CIRCULAR, MULTI_STEP }

@Serializable
data class ProgressSlotConfig(
    val kind: ProgressKind = ProgressKind.NONE,
    val valueBinding: FieldBinding = FieldBinding(NotificationField.PROGRESS),
    val activeColor: String? = null,
    val finishedColor: String? = null
)

@Serializable
data class ComposerActionButton(
    val label: String,
    val useNotificationAction: Boolean = false,
    val notificationActionIndex: Int = 0,
    val bgColor: String? = null
)

@Serializable
data class IslandTemplateDefinition(
    val templateType: ComposerTemplateType = ComposerTemplateType.T1_WEATHER_NAV,
    val leftGraphic: LeftGraphicSlot = LeftGraphicSlot(),
    val text: TextSlotConfig = TextSlotConfig(),
    val progress: ProgressSlotConfig = ProgressSlotConfig(),
    val buttons: List<ComposerActionButton> = emptyList(),
    val highlightColor: String? = null
)

/** Mirrors [com.d4viddf.hyperbridge.models.theme.RuleConditions] in shape, scoped to templates. */
@Serializable
data class TemplateRule(
    val packageNameRegex: String? = null,
    val titleRegex: String? = null,
    val textRegex: String? = null,
    val priority: Int = 100
)

@Serializable
data class ComposerTemplate(
    val id: String,
    val name: String,
    val definition: IslandTemplateDefinition,
    val rule: TemplateRule,
    val enabled: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)
