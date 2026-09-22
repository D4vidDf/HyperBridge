package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.SystemUpdateDesignConfig
import com.d4viddf.hyperbridge.models.SystemUpdateLeftDesign
import com.d4viddf.hyperbridge.models.SystemUpdateRightDesign
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import io.github.d4viddf.hyperisland_kit.models.CircularProgressInfo
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoRight
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.ProgressTextInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo

class SystemUpdateTranslator(
    context: Context,
    repo: ThemeRepository
) : BaseTranslator(context, repo) {

    private val finishKeywords by lazy {
        context.resources.getStringArray(R.array.progress_finish_keywords).toList()
    }

    fun translate(
        sbn: StatusBarNotification,
        picKey: String,
        config: IslandConfig,
        theme: HyperTheme?,
        isUpdate: Boolean,
        design: SystemUpdateDesignConfig = SystemUpdateDesignConfig()
    ): HyperIslandData {
        val extras = sbn.notification.extras

        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val versionText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim() ?: ""

        val effectiveTitle = rawTitle.ifEmpty { context.getString(R.string.system_updater_title) }

        val themeProgressColor = theme?.defaultProgress?.activeColor
            ?: resolveColor(theme, sbn.packageName, "#007AFF")

        val themeFinishColor = theme?.defaultProgress?.finishedColor
            ?: resolveColor(theme, sbn.packageName, "#34C759")

        val customTick = getThemeBitmap(theme, "tick_icon")

        val builder = HyperIslandNotification.Builder(context, "bridge_${sbn.packageName}", effectiveTitle)

        builder.setShowNotification(config.isShowShade ?: true)

        val isFloatEnabled = config.isFloat ?: false
        builder.setEnableFloat(isFloatEnabled && !isUpdate)
        builder.setIslandFirstFloat(config.isFloat ?: false)

        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val current = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val indeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE)

        val textPercent = extractTextPercentage(effectiveTitle, subText.ifEmpty { versionText })
        val percent = (if (max > 0) {
            ((current.toFloat() / max.toFloat()) * 100).toInt()
        } else {
            textPercent ?: 0
        }).coerceIn(0, 100)

        val isIndeterminate = indeterminate && textPercent == null
        val isActivelyProgressing = (max > 0 && current < max) || indeterminate
        val isTextFinished = !isActivelyProgressing && finishKeywords.any { 
            versionText.contains(it, ignoreCase = true) || rawTitle.contains(it, ignoreCase = true)
        }
        val isFinished = percent >= 100 || isTextFinished

        val tickKey = "${picKey}_tick"
        val hiddenKey = "hidden_pixel"
        val blankBadgeKey = "pic_blank_badge"

        builder.addPicture(resolveSystemUpdateIcon(sbn, picKey, design.iconSource))
        builder.addPicture(getTransparentPicture(hiddenKey))
        builder.addPicture(getColoredPicture(blankBadgeKey, R.drawable.ic_screen_recording_app_badge_blank, "#FFFFFF"))

        if (isFinished) {
            if (customTick != null) {
                builder.addPicture(HyperPicture(tickKey, customTick))
            } else {
                builder.addPicture(getColoredPicture(tickKey, R.drawable.rounded_check_circle_24, themeFinishColor))
            }
        }

        val actions = extractBridgeActions(sbn, config, theme)

        val displayContent = if (isFinished) {
            context.getString(R.string.download_complete)
        } else if (versionText.isNotEmpty()) {
            versionText
        } else {
            effectiveTitle
        }

        // Set chat info without package overlay badge using blank app badge
        builder.setChatInfo(
            title = effectiveTitle,
            content = displayContent,
            pictureKey = picKey,
            appPkg = blankBadgeKey
        )

        val hasProgress = max > 0 || indeterminate || textPercent != null

        if (!isFinished && !isIndeterminate && hasProgress) {
            builder.setProgressBar(percent, themeProgressColor)
        }

        if (isFinished) {
            builder.setBigIslandInfo(
                left = ImageTextInfoLeft(1, PicInfo(1, hiddenKey)),
                right = ImageTextInfoRight(2, PicInfo(1, tickKey))
            )
            builder.setSmallIsland(tickKey)
            builder.setIslandConfig(
                timeout = config.timeout,
                dismissible = true,
                expandedTimeMs = if (isFloatEnabled) config.floatTimeout else null
            )
        } else if (!hasProgress) {
            val compactText = versionText.ifEmpty { effectiveTitle }

            val leftInfo = when (design.left) {
                SystemUpdateLeftDesign.ICON_ONLY -> ImageTextInfoLeft(
                    type = 1,
                    picInfo = PicInfo(1, picKey)
                )
                SystemUpdateLeftDesign.ICON_AND_TEXT -> ImageTextInfoLeft(
                    type = 1,
                    picInfo = PicInfo(1, picKey),
                    textInfo = TextInfo(compactText, "")
                )
                SystemUpdateLeftDesign.TEXT_ONLY -> ImageTextInfoLeft(
                    type = 1,
                    textInfo = TextInfo(compactText, "")
                )
            }

            // In static notifications without progress (e.g. update available / restart phone),
            // if left design is ICON_ONLY, provide the title/content on the right so text isn't lost.
            val rightInfo = if (design.left == SystemUpdateLeftDesign.ICON_ONLY) {
                ImageTextInfoRight(
                    type = 1,
                    picInfo = PicInfo(1, hiddenKey),
                    textInfo = TextInfo(effectiveTitle, versionText)
                )
            } else {
                null
            }

            builder.setBigIslandInfo(left = leftInfo, right = rightInfo)
            builder.setSmallIsland(picKey)
            builder.setIslandConfig(
                timeout = config.timeout,
                dismissible = true,
                expandedTimeMs = if (isFloatEnabled) config.floatTimeout else null
            )
        } else {
            val compactText = versionText.ifEmpty { effectiveTitle }

            val leftInfo = when (design.left) {
                SystemUpdateLeftDesign.ICON_ONLY -> ImageTextInfoLeft(
                    type = 1,
                    picInfo = PicInfo(1, picKey)
                )
                SystemUpdateLeftDesign.ICON_AND_TEXT -> ImageTextInfoLeft(
                    type = 1,
                    picInfo = PicInfo(1, picKey),
                    textInfo = TextInfo(compactText, "")
                )
                SystemUpdateLeftDesign.TEXT_ONLY -> ImageTextInfoLeft(
                    type = 1,
                    textInfo = TextInfo(compactText, "")
                )
            }

            val rightInfo = when (design.right) {
                SystemUpdateRightDesign.PERCENTAGE -> ImageTextInfoRight(
                    type = 2,
                    textInfo = TextInfo(title = "$percent%", content = "")
                )
                SystemUpdateRightDesign.PROGRESS_CIRCLE -> null // Handled below by circular progress if preferred or custom
                SystemUpdateRightDesign.NONE -> null
            }

            if (design.right == SystemUpdateRightDesign.PROGRESS_CIRCLE) {
                if (design.left == SystemUpdateLeftDesign.TEXT_ONLY) {
                    val progressComponent = ProgressTextInfo(
                        progressInfo = CircularProgressInfo(progress = percent, colorReach = themeProgressColor, isCCW = true),
                        textInfo = null
                    )
                    builder.setBigIslandInfo(left = leftInfo, progressText = progressComponent)
                } else {
                    builder.setBigIslandProgressCircle(picKey, compactText, percent, themeProgressColor, true)
                }
            } else {
                builder.setBigIslandInfo(left = leftInfo, right = rightInfo)
            }

            builder.setSmallIslandCircularProgress(picKey, percent, themeProgressColor, isCCW = true)
        }

        val highlight = resolveColor(theme, sbn.packageName, themeProgressColor)
        builder.setIslandConfig(
            timeout = config.timeout,
            highlightColor = highlight,
            expandedTimeMs = config.floatTimeout
        )

        actions.forEach { it.actionImage?.let { pic -> builder.addPicture(pic) } }
        val hyperActions = actions.map { it.action }.toTypedArray()
        hyperActions.forEach {
            builder.addAction(it)
        }
        hyperActions.forEach { builder.addHiddenAction(it) }

        return HyperIslandData(builder.buildResourceBundle(), builder.buildJsonParam())
    }

    private fun resolveSystemUpdateIcon(
        sbn: StatusBarNotification,
        picKey: String,
        iconSource: com.d4viddf.hyperbridge.models.SystemUpdateIconSource
    ): HyperPicture {
        val bitmap = when (iconSource) {
            com.d4viddf.hyperbridge.models.SystemUpdateIconSource.NOTIFICATION_ICON -> {
                return resolveIcon(sbn, picKey)
            }
            com.d4viddf.hyperbridge.models.SystemUpdateIconSource.APP_PACKAGE_ICON -> {
                try {
                    val drawable = context.packageManager.getApplicationIcon(sbn.packageName)
                    drawable.toBitmap(width = 96, height = 96)
                } catch (_: Exception) {
                    null
                }
            }
            com.d4viddf.hyperbridge.models.SystemUpdateIconSource.PROVIDED_SYSTEM_UPDATE -> {
                try {
                    val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_system_update_arrow)
                    drawable?.toBitmap(width = 96, height = 96)
                } catch (_: Exception) {
                    null
                }
            }
            com.d4viddf.hyperbridge.models.SystemUpdateIconSource.PROVIDED_DOWNLOAD -> {
                try {
                    val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_system_update_download)
                    drawable?.toBitmap(width = 96, height = 96)
                } catch (_: Exception) {
                    null
                }
            }
        }
        return if (bitmap != null) {
            HyperPicture(picKey, bitmap)
        } else {
            resolveIcon(sbn, picKey)
        }
    }
}
