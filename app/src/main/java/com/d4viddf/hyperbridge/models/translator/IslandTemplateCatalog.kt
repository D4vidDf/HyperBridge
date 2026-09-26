package com.d4viddf.hyperbridge.models.translator

import androidx.annotation.StringRes
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.NotificationType
import java.util.UUID

/**
 * How Xiaomi lays a template out as a focus notification card, taken from the HyperIsland ToolKit
 * demo running on HyperOS 3. Previews draw from this so each template looks like it does on a phone.
 */
data class TemplateCardLayout(
    val style: Style,
    val progress: Progress = Progress.NONE,
    val hint: Hint = Hint.NONE,
    /** The template's own accent (ToolKit colours), used unless the design overrides the highlight. */
    val accentArgb: Long,
    /** Ticket-style card drawn over a tinted background picture (Template 9). */
    val tintedBackground: Boolean = false,
    /** Label of the hint row's button, when [hint] is [Hint.BUTTON]. */
    @StringRes val hintButtonRes: Int? = null
) {
    enum class Style {
        /** baseInfo type 1: small grey context line above a big accent title, picture on the right. */
        BASE_TYPE_1,
        /** baseInfo type 2: bold title line, grey content line, app picture on the right. */
        BASE_TYPE_2,
        /** chatInfo: avatar or thumbnail on the left, title and content, status glyph on the right. */
        CHAT,
        /** chatInfo with round decline / answer buttons. */
        CALL
    }

    enum class Progress { NONE, BAR, VEHICLE }

    /** The row under a divider: a label with a pill button, or a label over a big time. */
    enum class Hint { NONE, BUTTON, TIMER }
}

/**
 * One of Xiaomi's official Super Island templates (#272).
 *
 * A template is not a storage format of its own: it is a named [PresentationConfig] preset plus
 * the metadata needed to show it in the gallery. Picking one produces an ordinary
 * [CustomTranslator] with `presentation.mode = TEMPLATE`, so templates ride the Phase 3 translator
 * pipeline (matching, priority, .htrans import/export, theming) instead of a parallel one.
 */
data class IslandTemplate(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    val iconName: String,
    val suggestedTypes: List<NotificationType>,
    @StringRes val sampleTitleRes: Int,
    @StringRes val sampleTextRes: Int,
    @StringRes val sampleHighlightRes: Int? = null,
    /** T12 (compact media) is kept resolvable for existing translators but is not one of the ten. */
    val showInGallery: Boolean = true,
    /** Null for templates that are not one of the ten; those preview as a plain island. */
    val card: TemplateCardLayout? = null,
    val presentation: PresentationConfig
) {
    val showsProgress: Boolean get() = presentation.progressSlot.type != ProgressSlotType.NONE
    val showsActions: Boolean get() = presentation.actionSlots.any { it.isVisible }

    /** The translator a gallery card previews: this template applied to nothing in particular. */
    fun previewTranslator(): CustomTranslator = CustomTranslator(
        id = "preview_$id",
        meta = TranslatorMetadata(name = id),
        presentation = presentation.copy(mode = PresentationMode.TEMPLATE, templateId = id)
    )
}

object IslandTemplateCatalog {

    /**
     * The ten official templates, in Xiaomi's own order, plus the compact media player (T12) that
     * [com.d4viddf.hyperbridge.service.translators.DynamicTranslator] already recognises by id.
     */
    val all: List<IslandTemplate> = listOf(
        IslandTemplate(
            id = "tpl_weather_nav",
            nameRes = R.string.translator_pres_template_default,
            descriptionRes = R.string.island_template_desc_weather_nav,
            iconName = "Navigation",
            suggestedTypes = listOf(NotificationType.NAVIGATION, NotificationType.STANDARD),
            sampleTitleRes = R.string.island_template_sample_weather_nav_title,
            sampleTextRes = R.string.island_template_sample_weather_nav_text,
            sampleHighlightRes = R.string.island_template_sample_weather_nav_highlight,
            card = TemplateCardLayout(TemplateCardLayout.Style.BASE_TYPE_1, accentArgb = 0xFFFF3B30),
            presentation = PresentationConfig(
                textSlot = TextSlotConfig(
                    titleTemplate = "{notif.title}",
                    subtitleTemplate = "{notif.text}",
                    highlightTextTemplate = "{notif.subtext}"
                ),
                pill = CompactPillConfig(PillLeftDesign.ICON_AND_TEXT, PillRightDesign.HIGHLIGHT_TEXT)
            )
        ),
        IslandTemplate(
            id = "tpl_payment_wallet",
            nameRes = R.string.translator_pres_template_payment,
            descriptionRes = R.string.island_template_desc_payment,
            iconName = "Star",
            suggestedTypes = listOf(NotificationType.MESSAGE, NotificationType.STANDARD),
            sampleTitleRes = R.string.island_template_sample_payment_title,
            sampleTextRes = R.string.island_template_sample_payment_text,
            sampleHighlightRes = R.string.island_template_sample_payment_highlight,
            card = TemplateCardLayout(TemplateCardLayout.Style.BASE_TYPE_2, hint = TemplateCardLayout.Hint.BUTTON, accentArgb = 0xFFFF6900, hintButtonRes = R.string.island_template_hint_copy),
            presentation = PresentationConfig(
                textSlot = TextSlotConfig(
                    titleTemplate = "{notif.title}",
                    subtitleTemplate = "{notif.text}",
                    // Filled by DynamicTranslator's OTP extraction; nothing ever fills {regex.N}.
                    highlightTextTemplate = "{smart_action.OTP.code}"
                ),
                actionSlots = listOf(
                    ActionSlotConfig(
                        slotPosition = 0,
                        source = ActionSource.SMART_ACTION,
                        smartActionType = SmartActionType.OTP_COPY,
                        displayMode = ActionDisplayMode.ICON_AND_TEXT
                    )
                ),
                pill = CompactPillConfig(PillLeftDesign.ICON_AND_TEXT, PillRightDesign.HIGHLIGHT_TEXT)
            )
        ),
        IslandTemplate(
            id = "tpl_call_kit",
            nameRes = R.string.translator_pres_template_call,
            descriptionRes = R.string.island_template_desc_call,
            iconName = "Call",
            suggestedTypes = listOf(NotificationType.CALL),
            sampleTitleRes = R.string.island_template_sample_call_title,
            sampleTextRes = R.string.island_template_sample_call_text,
            card = TemplateCardLayout(TemplateCardLayout.Style.CALL, accentArgb = 0xFF34C759),
            presentation = PresentationConfig(
                leftSlot = SlotConfig(source = "AVATAR"),
                textSlot = TextSlotConfig(
                    titleTemplate = "{notif.title}",
                    subtitleTemplate = "{notif.text}"
                ),
                actionSlots = listOf(
                    ActionSlotConfig(
                        slotPosition = 0,
                        actionMatcher = ActionMatcher(actionIndex = 0),
                        displayMode = ActionDisplayMode.ICON_ONLY
                    ),
                    ActionSlotConfig(
                        slotPosition = 1,
                        actionMatcher = ActionMatcher(actionIndex = 1),
                        displayMode = ActionDisplayMode.ICON_ONLY
                    )
                ),
                pill = CompactPillConfig(PillLeftDesign.AVATAR, PillRightDesign.TIMER)
            )
        ),
        IslandTemplate(
            id = "tpl_ride_delivery",
            nameRes = R.string.translator_pres_template_delivery,
            descriptionRes = R.string.island_template_desc_delivery,
            iconName = "DirectionsCar",
            suggestedTypes = listOf(NotificationType.PROGRESS, NotificationType.STANDARD),
            sampleTitleRes = R.string.island_template_sample_delivery_title,
            sampleTextRes = R.string.island_template_sample_delivery_text,
            sampleHighlightRes = R.string.island_template_sample_delivery_highlight,
            card = TemplateCardLayout(TemplateCardLayout.Style.BASE_TYPE_2, progress = TemplateCardLayout.Progress.VEHICLE, accentArgb = 0xFF007AFF),
            presentation = PresentationConfig(
                textSlot = TextSlotConfig(
                    titleTemplate = "{notif.title}",
                    subtitleTemplate = "{notif.text}",
                    highlightTextTemplate = "{notif.subtext}"
                ),
                progressSlot = ProgressSlotConfig(type = ProgressSlotType.WAYPOINT, showPercentage = false),
                pill = CompactPillConfig(PillLeftDesign.ICON_AND_TEXT, PillRightDesign.HIGHLIGHT_TEXT)
            )
        ),
        IslandTemplate(
            id = "tpl_queue_wait",
            nameRes = R.string.translator_pres_template_queue,
            descriptionRes = R.string.island_template_desc_queue,
            iconName = "Speed",
            suggestedTypes = listOf(NotificationType.PROGRESS, NotificationType.STANDARD),
            sampleTitleRes = R.string.island_template_sample_queue_title,
            sampleTextRes = R.string.island_template_sample_queue_text,
            sampleHighlightRes = R.string.island_template_sample_queue_highlight,
            card = TemplateCardLayout(TemplateCardLayout.Style.BASE_TYPE_1, progress = TemplateCardLayout.Progress.BAR, accentArgb = 0xFFFF8514),
            presentation = PresentationConfig(
                textSlot = TextSlotConfig(
                    titleTemplate = "{notif.title}",
                    subtitleTemplate = "{notif.text}",
                    highlightTextTemplate = "{notif.subtext}"
                ),
                progressSlot = ProgressSlotConfig(type = ProgressSlotType.WAYPOINT, showPercentage = false),
                pill = CompactPillConfig(PillLeftDesign.ICON_AND_TEXT, PillRightDesign.HIGHLIGHT_TEXT)
            )
        ),
        IslandTemplate(
            id = "tpl_parking_meter",
            nameRes = R.string.translator_pres_template_parking,
            descriptionRes = R.string.island_template_desc_parking,
            iconName = "Timer",
            suggestedTypes = listOf(NotificationType.TIMER, NotificationType.PROGRESS),
            sampleTitleRes = R.string.island_template_sample_parking_title,
            sampleTextRes = R.string.island_template_sample_parking_text,
            card = TemplateCardLayout(TemplateCardLayout.Style.BASE_TYPE_2, progress = TemplateCardLayout.Progress.BAR, accentArgb = 0xFF34C759),
            presentation = PresentationConfig(
                textSlot = TextSlotConfig(
                    titleTemplate = "{notif.title}",
                    subtitleTemplate = "{notif.text}"
                ),
                progressSlot = ProgressSlotConfig(type = ProgressSlotType.TIMER),
                pill = CompactPillConfig(PillLeftDesign.ICON_AND_TEXT, PillRightDesign.TIMER)
            )
        ),
        IslandTemplate(
            id = "tpl_file_transfer",
            nameRes = R.string.translator_pres_template_download,
            descriptionRes = R.string.island_template_desc_transfer,
            iconName = "Download",
            suggestedTypes = listOf(NotificationType.DOWNLOAD, NotificationType.PROGRESS),
            sampleTitleRes = R.string.island_template_sample_transfer_title,
            sampleTextRes = R.string.island_template_sample_transfer_text,
            card = TemplateCardLayout(TemplateCardLayout.Style.CHAT, progress = TemplateCardLayout.Progress.BAR, accentArgb = 0xFF34C759),
            presentation = PresentationConfig(
                textSlot = TextSlotConfig(
                    titleTemplate = "{notif.title}",
                    subtitleTemplate = "{notif.text}"
                ),
                progressSlot = ProgressSlotConfig(type = ProgressSlotType.PROGRESS_BAR, showPercentage = true),
                pill = CompactPillConfig(PillLeftDesign.ICON_AND_TEXT, PillRightDesign.PROGRESS_PERCENT)
            )
        ),
        IslandTemplate(
            id = "tpl_promo_coupon",
            nameRes = R.string.translator_pres_template_promo,
            descriptionRes = R.string.island_template_desc_promo,
            iconName = "ShoppingBag",
            suggestedTypes = listOf(NotificationType.STANDARD),
            sampleTitleRes = R.string.island_template_sample_promo_title,
            sampleTextRes = R.string.island_template_sample_promo_text,
            sampleHighlightRes = R.string.island_template_sample_promo_highlight,
            card = TemplateCardLayout(TemplateCardLayout.Style.CHAT, hint = TemplateCardLayout.Hint.BUTTON, accentArgb = 0xFFFF8514, hintButtonRes = R.string.island_template_hint_view),
            presentation = PresentationConfig(
                textSlot = TextSlotConfig(
                    titleTemplate = "{notif.title}",
                    subtitleTemplate = "{notif.text}",
                    highlightTextTemplate = "{notif.subtext}"
                ),
                actionSlots = listOf(
                    ActionSlotConfig(
                        slotPosition = 0,
                        source = ActionSource.SMART_ACTION,
                        smartActionType = SmartActionType.OPEN_URL,
                        fallbackToSource = ActionSource.NOTIFICATION_ACTION,
                        displayMode = ActionDisplayMode.ICON_AND_TEXT
                    )
                ),
                pill = CompactPillConfig(PillLeftDesign.ICON_AND_TEXT, PillRightDesign.HIGHLIGHT_TEXT)
            )
        ),
        IslandTemplate(
            id = "tpl_boarding_pass",
            nameRes = R.string.island_template_boarding,
            descriptionRes = R.string.island_template_desc_boarding,
            iconName = "Flight",
            suggestedTypes = listOf(NotificationType.STANDARD),
            sampleTitleRes = R.string.island_template_sample_boarding_title,
            sampleTextRes = R.string.island_template_sample_boarding_text,
            sampleHighlightRes = R.string.island_template_sample_boarding_highlight,
            card = TemplateCardLayout(TemplateCardLayout.Style.BASE_TYPE_2, hint = TemplateCardLayout.Hint.TIMER, accentArgb = 0xFF6B504C, tintedBackground = true),
            presentation = PresentationConfig(
                textSlot = TextSlotConfig(
                    titleTemplate = "{notif.title}",
                    subtitleTemplate = "{notif.text}",
                    highlightTextTemplate = "{notif.subtext}"
                ),
                actionSlots = listOf(
                    ActionSlotConfig(
                        slotPosition = 0,
                        actionMatcher = ActionMatcher(actionIndex = 0),
                        displayMode = ActionDisplayMode.ICON_AND_TEXT
                    )
                ),
                pill = CompactPillConfig(PillLeftDesign.ICON_AND_TEXT, PillRightDesign.HIGHLIGHT_TEXT)
            )
        ),
        IslandTemplate(
            id = "tpl_courier_tracking",
            nameRes = R.string.island_template_courier,
            descriptionRes = R.string.island_template_desc_courier,
            iconName = "LocalShipping",
            suggestedTypes = listOf(NotificationType.PROGRESS, NotificationType.STANDARD),
            sampleTitleRes = R.string.island_template_sample_courier_title,
            sampleTextRes = R.string.island_template_sample_courier_text,
            sampleHighlightRes = R.string.island_template_sample_courier_highlight,
            card = TemplateCardLayout(TemplateCardLayout.Style.BASE_TYPE_2, progress = TemplateCardLayout.Progress.BAR, hint = TemplateCardLayout.Hint.BUTTON, accentArgb = 0xFF007AFF, hintButtonRes = R.string.island_template_hint_track),
            presentation = PresentationConfig(
                textSlot = TextSlotConfig(
                    titleTemplate = "{notif.title}",
                    subtitleTemplate = "{notif.text}",
                    highlightTextTemplate = "{notif.subtext}"
                ),
                progressSlot = ProgressSlotConfig(type = ProgressSlotType.WAYPOINT, showPercentage = false),
                actionSlots = listOf(
                    ActionSlotConfig(
                        slotPosition = 0,
                        source = ActionSource.SMART_ACTION,
                        smartActionType = SmartActionType.TRACK_PACKAGE,
                        fallbackToSource = ActionSource.NOTIFICATION_ACTION,
                        displayMode = ActionDisplayMode.ICON_AND_TEXT
                    )
                ),
                pill = CompactPillConfig(PillLeftDesign.ICON_AND_TEXT, PillRightDesign.HIGHLIGHT_TEXT)
            )
        ),
        IslandTemplate(
            id = "tpl_media_compact",
            nameRes = R.string.translator_pres_template_media,
            descriptionRes = R.string.island_template_desc_media,
            iconName = "MusicNote",
            suggestedTypes = listOf(NotificationType.MEDIA),
            sampleTitleRes = R.string.island_template_sample_media_title,
            sampleTextRes = R.string.island_template_sample_media_text,
            showInGallery = false,
            presentation = PresentationConfig(
                leftSlot = SlotConfig(source = "LARGE_ICON"),
                textSlot = TextSlotConfig(
                    titleTemplate = "{media.track}",
                    subtitleTemplate = "{media.artist}"
                ),
                pill = CompactPillConfig(PillLeftDesign.ICON_ONLY, PillRightDesign.NONE)
            )
        )
    )

    /** The ten templates offered when adding a design. */
    val gallery: List<IslandTemplate> = all.filter { it.showInGallery }

    fun find(templateId: String?): IslandTemplate? =
        templateId?.let { id -> all.firstOrNull { it.id == id } }

    /**
     * Renders a TEMPLATE translator that carries nothing but its `templateId` (an imported .htrans,
     * or one whose mode was just switched) as that template's preset.
     *
     * All or nothing on purpose. A slot-by-slot merge cannot tell "left at the default" from
     * "edited back to the default": editing one text field silently dropped the preset's other
     * ones (the highlight / OTP text), and deleting a preset's only action brought it back on the
     * next render. So a template is materialized once ([applyTemplate], [newDesign], or the editor
     * resolving it on load) and from then on its slots are rendered exactly as saved.
     */
    fun effectivePresentation(config: PresentationConfig): PresentationConfig {
        if (config.mode != PresentationMode.TEMPLATE || !isBare(config)) return config
        val preset = find(config.templateId)?.presentation ?: return config
        return preset.copy(
            mode = config.mode,
            templateId = config.templateId,
            widgetId = config.widgetId,
            rawParamV2 = config.rawParamV2
        )
    }

    /** Switches [config] to [templateId], with that template's own slots as the starting point. */
    fun applyTemplate(config: PresentationConfig, templateId: String): PresentationConfig {
        val preset = find(templateId)?.presentation
            ?: return config.copy(mode = PresentationMode.TEMPLATE, templateId = templateId)
        return preset.copy(
            mode = PresentationMode.TEMPLATE,
            templateId = templateId,
            widgetId = config.widgetId,
            rawParamV2 = config.rawParamV2
        )
    }

    /** True when every slot is still at its default, i.e. the config only names a template. */
    private fun isBare(config: PresentationConfig): Boolean {
        val defaults = PresentationConfig()
        return config.leftSlot == defaults.leftSlot &&
            config.textSlot == defaults.textSlot &&
            config.progressSlot == defaults.progressSlot &&
            config.actionSlots.isEmpty() &&
            config.pill == defaults.pill
    }

    /**
     * A ready-to-save design: the template, shown for one notification type and nothing else.
     * Everything finer grained (match conditions, per-element bindings) lives in the translator
     * editor afterwards.
     */
    fun newDesign(
        template: IslandTemplate,
        notificationType: NotificationType,
        name: String
    ): CustomTranslator = CustomTranslator(
        id = UUID.randomUUID().toString(),
        meta = TranslatorMetadata(name = name, author = LOCAL_DESIGN_AUTHOR, iconName = template.iconName),
        targetScope = TargetScope.NOTIFICATION_TYPE,
        targetNotificationTypes = listOf(notificationType.name),
        presentation = template.presentation.copy(
            mode = PresentationMode.TEMPLATE,
            templateId = template.id
        )
    )
}

/** Resolves the template preset before rendering, so the pipeline only ever sees full slots. */
fun CustomTranslator.withResolvedTemplate(): CustomTranslator {
    val resolved = IslandTemplateCatalog.effectivePresentation(presentation)
    return if (resolved == presentation) this else copy(presentation = resolved)
}

/** The author the translator editor stamps on anything created on this device. */
const val LOCAL_DESIGN_AUTHOR = "User"

/** A design is a translator that renders through a template or a custom widget island. */
val CustomTranslator.isDesign: Boolean
    get() = presentation.mode == PresentationMode.TEMPLATE || presentation.mode == PresentationMode.WIDGET

/** Designs made here carry [LOCAL_DESIGN_AUTHOR]; anything else arrived as an imported .htrans. */
val CustomTranslator.isImported: Boolean
    get() = meta.author != LOCAL_DESIGN_AUTHOR
