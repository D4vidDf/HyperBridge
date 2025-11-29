package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.NavContent
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoRight
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo

class NavTranslator(context: Context) : BaseTranslator(context) {

    private val arrivalKeywords by lazy { context.resources.getStringArray(R.array.nav_arrival_keywords).toList() }
    private val timeRegex = Regex("\\d{1,2}:\\d{2}")

    fun translate(
        sbn: StatusBarNotification,
        picKey: String,
        config: IslandConfig,
        leftLayout: NavContent,
        rightLayout: NavContent
    ): HyperIslandData {

        val extras = sbn.notification.extras

        // Sanitize inputs (Remove newlines that break layout)
        val title = (extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "").replace("\n", " ").trim()
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "").replace("\n", " ").trim()
        val subText = (extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: "").replace("\n", " ").trim()

        // 1. Progress
        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val current = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val hasProgress = max > 0
        val percent = if (hasProgress) ((current.toFloat() / max.toFloat()) * 100).toInt() else 0

        // 2. Text Analysis
        var instruction = ""
        var distance = ""
        var eta = ""

        fun isTimeInfo(s: String): Boolean = timeRegex.containsMatchIn(s) || arrivalKeywords.any { s.contains(it, true) }

        // Find ETA
        if (isTimeInfo(subText)) eta = subText
        if (text.isNotEmpty() && title.isNotEmpty()) {
            if (eta.isEmpty()) {
                if (isTimeInfo(text)) { eta = text; instruction = title }
                else if (isTimeInfo(title)) { eta = title; instruction = text }
            }
            if (instruction.isEmpty()) {
                if (title.length >= text.length) { instruction = title; distance = text }
                else { instruction = text; distance = title }
            }
        } else {
            instruction = if (title.isNotEmpty()) title else text
        }

        if (instruction.isEmpty()) instruction = context.getString(R.string.maps_title)

        val builder = HyperIslandNotification.Builder(context, "bridge_${sbn.packageName}", instruction)

        // --- CONFIG ---
        val finalTimeout = config.timeout ?: 5000L
        val shouldFloat = if (finalTimeout == 0L) false else (config.isFloat ?: true)

        builder.setEnableFloat(shouldFloat)
        builder.setTimeout(finalTimeout)
        builder.setShowNotification(config.isShowShade ?: true)

        // --- RESOURCES ---
        val hiddenKey = "hidden_pixel"
        val trackerIcon = resolveIcon(sbn, picKey)
        builder.addPicture(trackerIcon)
        builder.addPicture(getTransparentPicture(hiddenKey))

        val actions = extractBridgeActions(sbn)
        val actionKeys = actions.map { it.action.key }

        // Shade Info (Combined)
        val shadeContent = listOf(distance, eta).filter { it.isNotEmpty() }.joinToString(" • ")
        builder.setBaseInfo(
            title = instruction,
            content = shadeContent,
            pictureKey = picKey,
            actionKeys = actionKeys
        )

        // Progress (Shade Only)
        if (hasProgress) {
            builder.setProgressBar(
                progress = percent,
                color = "#34C759",
                picForwardKey = picKey
            )
        }

        // --- 3. BIG ISLAND CUSTOM LAYOUT ---

        // Helper to map Enum preference to Text Data
        fun getTextInfo(type: NavContent): TextInfo {
            return when (type) {
                NavContent.INSTRUCTION -> TextInfo(instruction, null) // Centered
                NavContent.DISTANCE -> TextInfo(distance, null)      // Centered
                NavContent.ETA -> TextInfo(eta, null)                // Centered
                NavContent.DISTANCE_ETA -> TextInfo(distance, eta)   // Top/Bottom
                NavContent.NONE -> TextInfo("", "")
            }
        }

        builder.setBigIslandInfo(
            // Left Side (Icon + User Configured Text)
            left = ImageTextInfoLeft(
                1,
                PicInfo(1, picKey),
                getTextInfo(leftLayout)
            ),
            // Right Side (User Configured Text + Transparent Spacer)
            right = ImageTextInfoRight(
                2,
                PicInfo(1, hiddenKey),
                getTextInfo(rightLayout)
            )
        )

        builder.setSmallIslandIcon(picKey)

        actions.forEach {
            builder.addAction(it.action)
            it.actionImage?.let { pic -> builder.addPicture(pic) }
        }

        return HyperIslandData(builder.buildResourceBundle(), builder.buildJsonParam())
    }
}