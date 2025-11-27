package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoRight
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo

class NavTranslator(context: Context) : BaseTranslator(context) {

    // Load keywords from resources (Localized)
    private val arrivalKeywords by lazy {
        context.resources.getStringArray(R.array.nav_arrival_keywords).toList()
    }

    private val timeRegex = Regex("\\d{1,2}:\\d{2}")

    fun translate(sbn: StatusBarNotification, picKey: String, config: IslandConfig): HyperIslandData {
        val extras = sbn.notification.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        // 1. Progress (Shade Only)
        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val current = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val hasProgress = max > 0
        val percent = if (hasProgress) ((current.toFloat() / max.toFloat()) * 100).toInt() else 0

        // 2. Text Analysis
        var instruction: String
        var distance = ""
        var eta = ""

        fun isTimeInfo(s: String): Boolean {
            return timeRegex.containsMatchIn(s) || arrivalKeywords.any { s.contains(it, true) }
        }

        // Find ETA
        if (isTimeInfo(subText)) eta = subText
        else if (isTimeInfo(text)) eta = text
        else if (isTimeInfo(title)) eta = title

        // Find Instruction vs Distance
        if (eta != text && eta != title) {
            if (text.isNotEmpty() && title.isNotEmpty()) {
                // Heuristic: Instruction is longer than distance
                if (title.length >= text.length) {
                    instruction = title
                    distance = text
                } else {
                    instruction = text
                    distance = title
                }
            } else {
                instruction = if (title.isNotEmpty()) title else text
            }
        } else {
            // One field was ETA, the other is Instruction
            instruction = if (eta == text) title else text
        }

        if (instruction.isEmpty()) instruction = context.getString(R.string.maps_title)

        val builder = HyperIslandNotification.Builder(context, "bridge_${sbn.packageName}", instruction)

        // --- CONFIG ---
        // Uncomment when library updated
        val finalTimeout = config.timeout ?: 5000L
        builder.setEnableFloat(config.isFloat?: true)
        builder.setTimeout(finalTimeout)
        builder.setShowNotification(config.isShowShade ?: true)

        // --- RESOURCES ---
        val hiddenKey = "hidden_pixel"
        val trackerIcon = resolveIcon(sbn, picKey)
        builder.addPicture(trackerIcon)
        builder.addPicture(getTransparentPicture(hiddenKey))

        val actions = extractBridgeActions(sbn)
        val actionKeys = actions.map { it.action.key }

        // --- 3. EXPANDED INFO (SHADE) ---
        val shadeContent = listOf(distance, eta).filter { it.isNotEmpty() }.joinToString(" • ")
        builder.setBaseInfo(
            title = instruction,
            content = shadeContent,
            pictureKey = picKey,
            actionKeys = actionKeys
        )

        // --- 4. BIG ISLAND (SPLIT LOGIC) ---
        var leftTitle: String
        var leftContent: String
        var rightText: String

        if (eta.isNotEmpty()) {
            // SCENARIO A: We have Time (ETA)
            // Right: [ Time ]
            // Left:  [ Arrow ] [ Instruction \n Distance ]
            rightText = eta
            leftTitle = instruction
            leftContent = distance
        } else {
            // SCENARIO B: No Time
            // Right: [ Distance ]
            // Left:  [ Arrow ] [ Instruction ]
            rightText = if (distance.isNotEmpty()) distance else ""
            leftTitle = instruction
            leftContent = ""
        }

        builder.setBigIslandInfo(
            // LEFT: Arrow + Text Block
            left = ImageTextInfoLeft(
                1,
                PicInfo(1, picKey),
                TextInfo(leftTitle, leftContent)
            ),
            // RIGHT: Specific Data (Time or Distance) + Transparent Spacer
            right = ImageTextInfoRight(
                2, // Type 2 = Vertically Centered Text
                PicInfo(1, hiddenKey),
                TextInfo(rightText, null)
            )
        )

        // Progress Bar (Shade Only)
        if (hasProgress) {
            builder.setProgressBar(
                progress = percent,
                color = "#34C759",
                picForwardKey = picKey
            )
        }

        builder.setSmallIslandIcon(picKey)

        actions.forEach {
            builder.addAction(it.action)
            it.actionImage?.let { pic -> builder.addPicture(pic) }
        }

        return HyperIslandData(builder.buildResourceBundle(), builder.buildJsonParam())
    }
}