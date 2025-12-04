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

        val title = (extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "").replace("\n", " ").trim()
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "").replace("\n", " ").trim()
        val subText = (extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: "").replace("\n", " ").trim()

        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val current = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val hasProgress = max > 0
        val percent = if (hasProgress) ((current.toFloat() / max.toFloat()) * 100).toInt() else 0

        var instruction = ""
        var distance = ""
        var eta = ""

        fun isTimeInfo(s: String): Boolean = timeRegex.containsMatchIn(s) || arrivalKeywords.any { s.contains(it, true) }

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
            instruction = title.ifEmpty { text }
        }

        if (instruction.isEmpty()) instruction = context.getString(R.string.maps_title)

        val builder = HyperIslandNotification.Builder(context, "bridge_${sbn.packageName}", instruction)

        val finalTimeout = config.timeout ?: 5000L
        val shouldFloat = if (finalTimeout == 0L) false else (config.isFloat ?: true)

        builder.setEnableFloat(shouldFloat)
        builder.setTimeout(finalTimeout)
        builder.setShowNotification(config.isShowShade ?: true)

        // --- RESOURCES ---
        val hiddenKey = "hidden_pixel"
        val navStartKey = "nav_start_icon"
        val navEndKey = "nav_end_icon"

        builder.addPicture(resolveIcon(sbn, picKey))
        builder.addPicture(getTransparentPicture(hiddenKey))
        builder.addPicture(getPictureFromResource(navStartKey, R.drawable.ic_nav_start))
        builder.addPicture(getPictureFromResource(navEndKey, R.drawable.ic_nav_end))

        // FIX: Use ActionDisplayMode.TEXT to prioritize text labels (Hint Info) over icons
        val actions = extractBridgeActions(sbn, ActionDisplayMode.TEXT)
        val actionKeys = actions.map { it.action.key }

        val shadeContent = listOf(distance, eta).filter { it.isNotEmpty() }.joinToString(" • ")

        // --- SHADE INFO ---
        builder.setBaseInfo(
            title = instruction,
            content = shadeContent,
            pictureKey = picKey,
            actionKeys = actionKeys,
            // FIX: Type 1 = Standard Template (Supports Text Buttons like "Exit")
            type = 1
        )

        if (hasProgress) {
            builder.setProgressBar(
                progress = percent,
                color = "#34C759",
                picForwardKey = navStartKey,
                picEndKey = navEndKey
            )
        }

        // --- ISLAND INFO ---
        fun getTextInfo(type: NavContent): TextInfo {
            return when (type) {
                NavContent.INSTRUCTION -> TextInfo(instruction, null)
                NavContent.DISTANCE -> TextInfo(distance, null)
                NavContent.ETA -> TextInfo(eta, null)
                NavContent.DISTANCE_ETA -> TextInfo(distance, eta)
                NavContent.NONE -> TextInfo("", "")
            }
        }

        builder.setBigIslandInfo(
            left = ImageTextInfoLeft(
                1,
                PicInfo(1, picKey),
                getTextInfo(leftLayout)
            ),
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