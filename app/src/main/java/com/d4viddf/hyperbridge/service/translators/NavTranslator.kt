package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.content.Context
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.graphics.toColorInt
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.NavContent
import io.github.d4viddf.hyperisland_kit.HyperAction
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoRight
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo

class NavTranslator(context: Context) : BaseTranslator(context) {

    private val TAG = "HyperBridgeNav"
    private val arrivalKeywords by lazy { context.resources.getStringArray(R.array.nav_arrival_keywords).toList() }

    private val timeRegex = Regex("(\\d{1,2}:\\d{2})|(\\d+h\\s*\\d+m)", RegexOption.IGNORE_CASE)
    private val distanceRegex = Regex("^\\d+([,.]\\d+)?\\s*(m|km|ft|mi|yd|yards|miles|meters)", RegexOption.IGNORE_CASE)

    fun translate(
        sbn: StatusBarNotification,
        picKey: String,
        config: IslandConfig,
        leftLayout: NavContent,
        rightLayout: NavContent
    ): HyperIslandData {

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

        // --- 1. Extract ETA ---
        if (isTimeInfo(subText)) {
            eta = subText
        } else if (isTimeInfo(text) && !isDistanceInfo(text)) {
            eta = text
        }

        // --- 2. Smart Parsing ---
        val separators = listOf("·", "•", "-")
        val candidates = listOf(bigText, title, text).filter { it.isNotEmpty() }

        val contentSource = candidates.firstOrNull { str ->
            separators.any { str.contains(it) } && isDistanceInfo(str)
        } ?: candidates.firstOrNull { str ->
            isDistanceInfo(str)
        } ?: if (title.isNotEmpty()) title else text

        var splitSuccess = false

        for (sep in separators) {
            if (contentSource.contains(sep)) {
                val parts = contentSource.split(sep, limit = 2)
                if (parts.size >= 2) {
                    val p0 = parts[0].trim()
                    val p1 = parts[1].trim()

                    if (isDistanceInfo(p0)) {
                        distance = p0
                        instruction = p1
                        splitSuccess = true
                        break
                    }
                }
            }
        }

        if (!splitSuccess && isDistanceInfo(contentSource)) {
            val match = distanceRegex.find(contentSource)
            if (match != null && match.range.start == 0) {
                distance = match.value.trim()
                instruction = contentSource.substring(match.range.endInclusive + 1).trim()
                val cleanInstruction = instruction.trimStart { it == '·' || it == '•' || it == '-' || it.isWhitespace() }
                instruction = cleanInstruction
                splitSuccess = true
            }
        }

        if (!splitSuccess) {
            if (title.isNotEmpty() && text.isNotEmpty()) {
                if (isDistanceInfo(title)) {
                    distance = title
                    instruction = text
                } else if (isDistanceInfo(text)) {
                    distance = text
                    instruction = title
                } else {
                    instruction = title
                }
            } else {
                instruction = contentSource
            }
        }

        if (instruction.isEmpty()) instruction = context.getString(R.string.maps_title)

        Log.d(TAG, "NavData -> Inst: '$instruction' | Dist: '$distance' | ETA: '$eta'")

        val builder = HyperIslandNotification.Builder(context, "bridge_${sbn.packageName}", instruction)

        val finalTimeout = config.timeout ?: 0
        builder.setEnableFloat(config.isFloat ?: false)
        builder.setTimeout(finalTimeout)
        builder.setShowNotification(config.isShowShade ?: true)

        val hiddenKey = "hidden_pixel"
        val navStartKey = "nav_start_icon"
        val navEndKey = "nav_end_icon"

        builder.addPicture(resolveIcon(sbn, picKey))
        builder.addPicture(getTransparentPicture(hiddenKey))
        builder.addPicture(getPictureFromResource(navStartKey, R.drawable.ic_nav_start))
        builder.addPicture(getPictureFromResource(navEndKey, R.drawable.ic_nav_end))

        // --- ACTION BUTTONS (Manual Processing for Custom Backgrounds) ---
        val rawActions = sbn.notification.actions ?: emptyArray()
        val bgGreyTransparent = "#40808080".toColorInt() // ~25% Grey

        rawActions.forEachIndexed { index, action ->
            val uniqueKey = "act_${sbn.key.hashCode()}_$index"

            // 1. Load Original Icon Bitmap
            val originalIcon = action.getIcon()
            val originalBitmap = if (originalIcon != null) loadIconBitmap(originalIcon, sbn.packageName) else null

            var actionIcon: Icon? = null
            var hyperPic: HyperPicture? = null

            // 2. Apply Grey Background & Padding
            if (originalBitmap != null) {
                val roundedBitmap = createRoundedIconWithBackground(
                    source = originalBitmap,
                    backgroundColor = bgGreyTransparent,
                    paddingDp = 6
                )
                val picKeyAction = "${uniqueKey}_icon"

                actionIcon = Icon.createWithBitmap(roundedBitmap)
                hyperPic = HyperPicture(picKeyAction, roundedBitmap)
            }

            // 3. Create Action (No background color, white text)
            val hyperAction = HyperAction(
                key = uniqueKey,
                title = action.title?.toString() ?: "",
                icon = actionIcon,
                pendingIntent = action.actionIntent,
                actionIntentType = 1,
                actionBgColor = null, // Disable pill background so our circle shows
                titleColor = "#FFFFFF"
            )

            // 4. Add to Builder
            builder.addAction(hyperAction)
            hyperPic?.let { builder.addPicture(it) }
        }

        // --- SHADE INFO ---
        val finalEta = eta.ifEmpty { " " }
        val finalDistance = distance.ifEmpty { " " }

        builder.setCoverInfo(
            picKey = picKey,
            title = instruction,
            content = finalEta,
            subContent = finalDistance
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

        builder.setSmallIsland(picKey)

        return HyperIslandData(builder.buildResourceBundle(), builder.buildJsonParam())
    }
}