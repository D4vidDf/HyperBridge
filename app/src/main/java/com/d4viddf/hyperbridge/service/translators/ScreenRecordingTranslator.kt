package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.ScreenRecordingDesignConfig
import com.d4viddf.hyperbridge.models.ScreenRecordingLeftDesign
import com.d4viddf.hyperbridge.models.ScreenRecordingRightDesign
import com.d4viddf.hyperbridge.receiver.ScreenRecordingActionReceiver
import com.d4viddf.hyperbridge.service.recording.ScreenRecordingSession
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ScreenRecordingTranslator(private val context: Context) {
    fun translate(
        session: ScreenRecordingSession,
        now: Long = System.currentTimeMillis(),
        design: ScreenRecordingDesignConfig = ScreenRecordingDesignConfig()
    ): HyperIslandData {
        val canStop = session.capabilities.canStop
        val payload = ScreenRecordingPayloadFactory.build(
            session = session,
            now = now,
            compactText = context.getString(R.string.screen_recording_compact),
            expandedText = context.getString(R.string.screen_recording_active),
            notifyId = "${context.packageName}:${session.logicalId.hashCode()}",
            design = design
        )

        return HyperIslandData(
            resources = buildResources(session, canStop),
            jsonParam = payload
        )
    }

    private fun buildResources(session: ScreenRecordingSession, canStop: Boolean): Bundle {
        val pictures = Bundle().apply {
            putParcelable(PIC_TICKER, Icon.createWithResource(context, R.drawable.ic_screen_recording_ticker))
            putParcelable(
                PIC_APP_BADGE,
                Icon.createWithResource(context, R.drawable.ic_screen_recording_app_badge_blank)
            )
            if (canStop) {
                putParcelable(PIC_STOP, Icon.createWithResource(context, R.drawable.ic_screen_recording_stop_light))
                putParcelable(PIC_STOP_DARK, Icon.createWithResource(context, R.drawable.ic_screen_recording_stop_dark))
            }
        }

        return Bundle().apply {
            putBundle("miui.focus.pics", pictures)
            if (canStop) {
                val stopIntent = Intent(context, ScreenRecordingActionReceiver::class.java).apply {
                    action = ScreenRecordingActionReceiver.ACTION_STOP
                    putExtra(EXTRA_SESSION_ID, session.logicalId)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    session.logicalId.hashCode(),
                    stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val stopAction = Notification.Action.Builder(
                    null,
                    context.getString(R.string.screen_recording_stop),
                    pendingIntent
                ).build()
                putBundle("miui.focus.actions", Bundle().apply {
                    putParcelable(ACTION_STOP, stopAction)
                })
            }
        }
    }

    companion object {
        const val BUSINESS = "screen_recording"
        const val SCENE = "recorder"
        const val HIGHLIGHT_COLOR = "#FB382F"
        const val ISLAND_TIMEOUT_SECONDS = 43_200
        const val TIMER_TYPE_COUNT_UP = 1
        const val ACTION_STOP = "miui.focus.action_1"
        const val PIC_TICKER = "miui.focus.pic_ticker"
        const val PIC_APP_BADGE = "miui.focus.pic_recorder_app_badge"
        const val PIC_STOP = "miui.focus.pic_stop"
        const val PIC_STOP_DARK = "miui.focus.pic_stop_dark"
        const val EXTRA_SESSION_ID = "screen_recording_session_id"
    }
}

internal object ScreenRecordingPayloadFactory {
    private val json = Json {
        encodeDefaults = false
        explicitNulls = false
    }

    fun build(
        session: ScreenRecordingSession,
        now: Long,
        compactText: String,
        expandedText: String,
        notifyId: String,
        design: ScreenRecordingDesignConfig = ScreenRecordingDesignConfig()
    ): String {
        val timerInfo = RecorderTimerInfo(
            timerWhen = session.startedAt,
            timerType = ScreenRecordingTranslator.TIMER_TYPE_COUNT_UP,
            timerSystemCurrent = now
        )
        val chatTimerInfo = RecorderChatTimerInfo(
            timerWhen = session.startedAt,
            timerType = ScreenRecordingTranslator.TIMER_TYPE_COUNT_UP,
            timerTotal = session.startedAt,
            timerSystemCurrent = now
        )
        val imageTextInfoLeft = when (design.left) {
            ScreenRecordingLeftDesign.ICON_ONLY -> RecorderImageTextInfo(
                type = 1,
                picInfo = RecorderPicInfo(
                    type = 1,
                    pic = ScreenRecordingTranslator.PIC_TICKER
                ),
                textInfo = null
            )
            ScreenRecordingLeftDesign.ICON_AND_TEXT -> RecorderImageTextInfo(
                type = 1,
                picInfo = RecorderPicInfo(
                    type = 1,
                    pic = ScreenRecordingTranslator.PIC_TICKER
                ),
                textInfo = RecorderTextInfo(title = compactText, content = "")
            )
            ScreenRecordingLeftDesign.TEXT_ONLY -> RecorderImageTextInfo(
                type = 1,
                picInfo = RecorderPicInfo(
                    type = 1,
                    pic = ScreenRecordingTranslator.PIC_APP_BADGE
                ),
                textInfo = RecorderTextInfo(title = compactText, content = "")
            )
        }
        val sameWidthDigitInfo = when (design.right) {
            ScreenRecordingRightDesign.TIMER -> RecorderSameWidthDigitInfo(timerInfo = timerInfo)
            ScreenRecordingRightDesign.NONE -> null
        }
        val payload = RecorderFocusRoot(
            paramV2 = RecorderParamV2(
                protocol = 1,
                updatable = true,
                enableFloat = false,
                business = ScreenRecordingTranslator.BUSINESS,
                scene = ScreenRecordingTranslator.SCENE,
                content = "",
                notifyId = notifyId,
                showSmallIcon = false,
                hideDeco = true,
                islandFirstFloat = false,
                ticker = "",
                tickerPic = ScreenRecordingTranslator.PIC_TICKER,
                tickerPicDark = ScreenRecordingTranslator.PIC_TICKER,
                paramIsland = RecorderParamIsland(
                    islandPriority = 1,
                    islandTimeout = ScreenRecordingTranslator.ISLAND_TIMEOUT_SECONDS,
                    islandProperty = 2,
                    highlightColor = ScreenRecordingTranslator.HIGHLIGHT_COLOR,
                    bigIslandArea = RecorderBigIslandArea(
                        imageTextInfoLeft = imageTextInfoLeft,
                        sameWidthDigitInfo = sameWidthDigitInfo
                    ),
                    smallIslandArea = RecorderSmallIslandArea(
                        RecorderPicInfo(type = 1, pic = ScreenRecordingTranslator.PIC_TICKER)
                    )
                ),
                chatInfo = RecorderChatInfo(
                    type = 1,
                    title = expandedText,
                    timerInfo = chatTimerInfo,
                    picProfile = ScreenRecordingTranslator.PIC_TICKER,
                    picProfileDark = ScreenRecordingTranslator.PIC_TICKER,
                    appIconPkg = ScreenRecordingTranslator.PIC_APP_BADGE
                ),
                actions = if (session.capabilities.canStop) {
                    listOf(
                        RecorderActionRef(
                            actionIntentType = 0,
                            action = ScreenRecordingTranslator.ACTION_STOP,
                            type = 0,
                            actionIcon = ScreenRecordingTranslator.PIC_STOP,
                            actionIconDark = ScreenRecordingTranslator.PIC_STOP_DARK
                        )
                    )
                } else {
                    null
                }
            )
        )

        return json.encodeToString(payload)
    }
}

@Serializable
private data class RecorderFocusRoot(
    @SerialName("param_v2") val paramV2: RecorderParamV2
)

@Serializable
private data class RecorderParamV2(
    val protocol: Int,
    val updatable: Boolean,
    val enableFloat: Boolean,
    val business: String,
    val scene: String,
    val content: String,
    val notifyId: String,
    val showSmallIcon: Boolean,
    val hideDeco: Boolean,
    val islandFirstFloat: Boolean,
    val ticker: String,
    val tickerPic: String,
    val tickerPicDark: String,
    @SerialName("param_island") val paramIsland: RecorderParamIsland,
    val chatInfo: RecorderChatInfo,
    val actions: List<RecorderActionRef>? = null
)

@Serializable
private data class RecorderParamIsland(
    val islandPriority: Int,
    val islandTimeout: Int,
    val islandProperty: Int,
    val highlightColor: String,
    val bigIslandArea: RecorderBigIslandArea,
    val smallIslandArea: RecorderSmallIslandArea
)

@Serializable
private data class RecorderBigIslandArea(
    val imageTextInfoLeft: RecorderImageTextInfo,
    val sameWidthDigitInfo: RecorderSameWidthDigitInfo? = null
)

@Serializable
private data class RecorderSmallIslandArea(val picInfo: RecorderPicInfo)

@Serializable
private data class RecorderImageTextInfo(
    val type: Int,
    val picInfo: RecorderPicInfo,
    val textInfo: RecorderTextInfo? = null
)

@Serializable
private data class RecorderTextInfo(val title: String, val content: String)

@Serializable
private data class RecorderSameWidthDigitInfo(
    val timerInfo: RecorderTimerInfo,
    val content: String? = null
)

@Serializable
private data class RecorderTimerInfo(
    val timerWhen: Long,
    val timerType: Int,
    val timerSystemCurrent: Long
)

@Serializable
private data class RecorderChatInfo(
    val type: Int,
    val title: String,
    val timerInfo: RecorderChatTimerInfo,
    @SerialName("picProfile") val picProfile: String,
    @SerialName("picProfileDark") val picProfileDark: String,
    @SerialName("appIconPkg") val appIconPkg: String
)

@Serializable
private data class RecorderChatTimerInfo(
    val timerWhen: Long,
    val timerType: Int,
    val timerTotal: Long,
    val timerSystemCurrent: Long
)

@Serializable
private data class RecorderPicInfo(
    val type: Int,
    val pic: String,
    val loop: Boolean = false,
    val autoplay: Boolean = false,
    val number: Int = 0
)

@Serializable
private data class RecorderActionRef(
    val actionIntentType: Int,
    val action: String,
    val type: Int,
    val actionIcon: String,
    val actionIconDark: String
)
