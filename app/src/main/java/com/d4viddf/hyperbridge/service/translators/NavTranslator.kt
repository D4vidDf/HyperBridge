package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.NavContent
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import io.github.d4viddf.hyperisland_kit.HyperAction
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoRight
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo

class NavTranslator(context: Context, repo: ThemeRepository) : BaseTranslator(context, repo) {

    private val timeRegex = Regex("(\\d{1,2}:\\d{2})|(\\d+h\\s*\\d+m)", RegexOption.IGNORE_CASE)
    private val distanceRegex = Regex("^\\d+([,.]\\d+)?\\s*(m|km|ft|mi|yd|yards|miles|meters)", RegexOption.IGNORE_CASE)
    private val arrivalKeywords by lazy { context.resources.getStringArray(R.array.nav_arrival_keywords).toList() }

    fun translate(
        sbn: StatusBarNotification,
        picKey: String,
        config: IslandConfig,
        leftLayout: NavContent,
        rightLayout: NavContent,
        theme: HyperTheme?
    ): HyperIslandData {

        // 1. Resolve Theme Colors
        val themeProgressBarColor = theme?.defaultNavigation?.progressBarColor
            ?: resolveColor(theme, sbn.packageName, "#34C759")

        // 2. Resolve Custom Icons
        val navStartBitmap = getThemeBitmap(theme, "nav_start")
        val navEndBitmap = getThemeBitmap(theme, "nav_end")

        // 3. Parse Notification Content
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.replace("\n", " ")?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.replace("\n", " ")?.trim() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.replace("\n", " ")?.trim() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.replace("\n", " ")?.trim() ?: ""

        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val current = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val hasProgress = max > 0
        val percent = if (hasProgress) ((current.toFloat() / max.toFloat()) * 100).toInt() else 0

        var instruction = ""
        var distance = ""
        var eta = ""

        fun isTimeInfo(s: String): Boolean = timeRegex.containsMatchIn(s) || arrivalKeywords.any { s.contains(it, true) }
        fun isDistanceInfo(s: String): Boolean = distanceRegex.containsMatchIn(s)

        // logic to extract ETA/Distance from various fields (Waze vs Maps vs Others)
        if (isTimeInfo(subText)) eta = subText
        else if (isTimeInfo(text) && !isDistanceInfo(text)) eta = text

        val candidates = listOf(bigText, title, text).filter { it.isNotEmpty() }
        val contentSource = candidates.firstOrNull { str -> distanceRegex.containsMatchIn(str) } ?: if (title.isNotEmpty()) title else text

        if (isDistanceInfo(contentSource)) {
            val match = distanceRegex.find(contentSource)
            if (match != null) {
                distance = match.value
                instruction = contentSource.replace(distance, "").trim { it == '·' || it == '-' || it.isWhitespace() }
            }
        } else {
            instruction = contentSource
        }

        if (instruction.isEmpty()) instruction = context.getString(R.string.maps_title)

        // 4. Build Notification
        val builder = HyperIslandNotification.Builder(context, "bridge_${sbn.packageName}", instruction)
        builder.setEnableFloat(config.isFloat ?: false)
        builder.setIslandConfig(timeout = config.timeout)
        builder.setShowNotification(config.isShowShade ?: true)
        builder.setIslandFirstFloat(config.isFloat ?: false)

        val hiddenKey = "hidden_pixel"
        val navStartKey = "nav_start_icon"
        val navEndKey = "nav_end_icon"

        // Add Images
        builder.addPicture(resolveIcon(sbn, picKey))
        builder.addPicture(getTransparentPicture(hiddenKey))

        if (navStartBitmap != null) {
            builder.addPicture(HyperPicture(navStartKey, navStartBitmap))
        } else {
            builder.addPicture(getColoredPicture(navStartKey, R.drawable.ic_nav_start, themeProgressBarColor))
        }

        if (navEndBitmap != null) {
            builder.addPicture(HyperPicture(navEndKey, navEndBitmap))
        } else {
            builder.addPicture(getColoredPicture(navEndKey, R.drawable.ic_nav_end, themeProgressBarColor))
        }

        // 5. Actions (Important: Nav needs Text Buttons like "Exit")
        // We use extractBridgeActions from BaseTranslator but customize the mode slightly if needed
        val rawActions = sbn.notification.actions ?: emptyArray()
        val actionKeys = mutableListOf<String>()

        rawActions.forEachIndexed { index, action ->
            val uniqueKey = "act_${sbn.key.hashCode()}_$index"

            // For Navigation, we usually prefer Text buttons (e.g. "Exit Navigation")
            // So we don't necessarily need the fancy icon shape logic here unless user explicitly styles it.
            // We pass null for background to keep it standard pill style or text only.

            val hyperAction = HyperAction(
                key = uniqueKey,
                title = action.title?.toString() ?: "",
                icon = null, // Navigation usually works better with text-only pills in the shade
                pendingIntent = action.actionIntent,
                actionIntentType = 1,
                actionBgColor = null, // Transparent/Default
                titleColor = "#FFFFFF"
            )

            builder.addAction(hyperAction)
            actionKeys.add(uniqueKey)
        }

        // 6. Shade Layout (The Fix)
        // Revert to setBaseInfo (Type 1) which supports standard notifications with actions.
        val shadeContent = listOf(distance, eta).filter { it.isNotEmpty() }.joinToString(" • ")

        builder.setBaseInfo(
            type = 1, // Standard Template
            title = instruction,
            content = shadeContent,
            pictureKey = picKey,
            actionKeys = actionKeys
        )

        // 7. Progress Bar
        if (hasProgress) {
            builder.setProgressBar(
                progress = percent,
                color = themeProgressBarColor,
                picForwardKey = navStartKey,
                picEndKey = navEndKey
            )
        }

        // 8. Island Layout (Dynamic)
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
            left = ImageTextInfoLeft(1, PicInfo(1, picKey), getTextInfo(leftLayout)),
            right = ImageTextInfoRight(2, PicInfo(1, hiddenKey), getTextInfo(rightLayout))
        )

        builder.setSmallIsland(picKey)
        builder.setIslandConfig(highlightColor = theme?.global?.highlightColor)

        return HyperIslandData(builder.buildResourceBundle(), builder.buildJsonParam())
    }
}