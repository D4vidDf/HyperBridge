package com.d4viddf.hyperbridge.models.widget

import com.d4viddf.hyperbridge.data.widget.WidgetVariableEngine
import com.d4viddf.hyperbridge.ui.screens.design.studio.StudioPreviewScenario
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewPlaceholderConfigTest {

    private val engine = WidgetVariableEngine()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @Test
    fun allScenarioPresetsRoundTripThroughJsonAndMatchScenario() {
        StudioPreviewScenario.entries.forEach { scenario ->
            val config = PreviewPlaceholderConfig.fromScenario(scenario)
            assertFalse(config.isCustomized)
            assertEquals(scenario, config.matchingScenarioOrNull())

            val encoded = json.encodeToString(PreviewPlaceholderConfig.serializer(), config)
            val decoded = json.decodeFromString(PreviewPlaceholderConfig.serializer(), encoded)
            assertEquals(config, decoded)
        }
    }

    @Test
    fun customPresetDefaultsAllOptionsToEmpty() {
        val emptyConfig = PreviewPlaceholderConfig.fromScenario(StudioPreviewScenario.CUSTOM)
        assertEquals(StudioPreviewScenario.CUSTOM.name, emptyConfig.presetName)
        assertFalse(emptyConfig.isCustomized)
        assertEquals("", emptyConfig.notifTitle)
        assertEquals("", emptyConfig.notifText)
        assertEquals("", emptyConfig.subtext)
        assertEquals("", emptyConfig.sender)
        assertEquals("", emptyConfig.conversation)
        assertEquals("", emptyConfig.appName)
        assertEquals("", emptyConfig.notifPackage)
        assertNull(emptyConfig.pictureRef)
        assertNull(emptyConfig.avatarRef)
        assertNull(emptyConfig.smallIconRef)
        assertEquals("", emptyConfig.mediaTrack)
        assertEquals("", emptyConfig.mediaArtist)
        assertEquals("", emptyConfig.mediaPosition)
        assertEquals("", emptyConfig.mediaDuration)
        assertEquals(0, emptyConfig.mediaProgress)
        assertFalse(emptyConfig.isMediaPlaying)
        assertFalse(emptyConfig.hasNotifProgress)
        assertEquals("", emptyConfig.timerText)
        assertEquals("", emptyConfig.countdownText)
        assertTrue(emptyConfig.notificationActionTitles.isEmpty())
        assertFalse(emptyConfig.hasInlineReply)
        assertTrue(emptyConfig.smartActionTypes.isEmpty())
        assertEquals("", emptyConfig.timeNowFormatted)
        assertEquals(0, emptyConfig.deviceBatteryPercent)

        val ctx = emptyConfig.toVariableContext()
        assertEquals("", engine.resolve("{notif.title}", ctx))
        assertEquals("", engine.resolve("{media.track}", ctx))
        assertNull(ctx.notifPackage)
        assertNull(ctx.notifProgress)

        val sample = emptyConfig.toIslandPreviewSample()
        assertEquals("", sample.title)
        assertEquals("", sample.text)
        assertEquals("", sample.app)
        assertEquals("", sample.clockText)
    }

    @Test
    fun customPlaceholdersResolveVariablesAndConditionsAccurately() {
        val custom = PreviewPlaceholderConfig.fromScenario(StudioPreviewScenario.STANDARD).copy(
            isCustomized = true,
            notifTitle = "Custom Song Alert",
            notifText = "Live at Wembley",
            subtext = "991827",
            sender = "DJ Nova",
            appName = "YouTube Music",
            notifPackage = "com.google.android.apps.youtube.music",
            mediaTrack = "Starlight",
            mediaArtist = "Muse",
            mediaPosition = "02:10",
            mediaDuration = "04:00",
            mediaProgress = 54,
            isMediaPlaying = true,
            mediaState = "PLAYING",
            hasNotifProgress = true,
            notifProgress = 82,
            hasInlineReply = true,
            smartActionTypes = setOf("OTP", "URL"),
            notificationActionTitles = listOf("Like", "Skip"),
            deviceBatteryPercent = 41,
            timeNowFormatted = "23:59",
            sourceValues = mapOf("weather.temp" to "19°C", "weather.text" to "Clear Sky")
        )

        val ctx = custom.toVariableContext()
        assertEquals("Custom Song Alert", engine.resolve("{notif.title}", ctx))
        assertEquals("Live at Wembley", engine.resolve("{notif.text}", ctx))
        assertEquals("82", engine.resolve("{notif.progress}", ctx))
        assertEquals("com.google.android.apps.youtube.music", engine.resolve("{notif.package}", ctx))
        assertEquals("Starlight - Muse", engine.resolve("{media.track} - {media.artist}", ctx))
        assertEquals("02:10 / 04:00", engine.resolve("{media.position} / {media.duration}", ctx))
        assertEquals("54", engine.resolve("{media.progress}", ctx))
        assertEquals("41", engine.resolve("{device.battery}", ctx))
        assertEquals("23:59", engine.resolve("{time.now}", ctx))
        assertEquals("19°C (Clear Sky)", engine.resolve("{source.weather.temp} ({source.weather.text})", ctx))

        assertTrue(NodeConditionEvaluator.isVisible(NodeCondition.HasProgress, ctx, engine))
        assertTrue(NodeConditionEvaluator.isVisible(NodeCondition.HasInlineReply, ctx, engine))
        assertTrue(NodeConditionEvaluator.isVisible(NodeCondition.HasSmartAction("OTP"), ctx, engine))
        assertTrue(NodeConditionEvaluator.isVisible(NodeCondition.HasSmartAction("url"), ctx, engine))
        assertFalse(NodeConditionEvaluator.isVisible(NodeCondition.HasSmartAction("PHONE"), ctx, engine))
        assertTrue(NodeConditionEvaluator.isVisible(NodeCondition.HasNotificationAction(1), ctx, engine))
        assertFalse(NodeConditionEvaluator.isVisible(NodeCondition.HasNotificationAction(2), ctx, engine))

        val sample = custom.toIslandPreviewSample()
        assertEquals("Custom Song Alert", sample.title)
        assertEquals("Live at Wembley", sample.text)
        assertEquals("991827", sample.subtext)
        assertEquals("DJ Nova", sample.sender)
        assertEquals("YouTube Music", sample.app)
        assertEquals("Starlight", sample.track)
        assertEquals("Muse", sample.artist)
        assertEquals(82, sample.progressPercent)
        assertEquals("23:59", sample.clockText)
    }

    @Test
    fun disablingNotifProgressClearsVariableContextProgressAndFallsBackToMediaProgressForSample() {
        val config = PreviewPlaceholderConfig.fromScenario(StudioPreviewScenario.MEDIA).copy(
            hasNotifProgress = false,
            notifProgress = 90,
            mediaProgress = 37
        )
        val ctx = config.toVariableContext()
        assertNull(ctx.notifProgress)
        assertFalse(NodeConditionEvaluator.isVisible(NodeCondition.HasProgress, ctx, engine))

        val sample = config.toIslandPreviewSample()
        assertEquals(37, sample.progressPercent)
    }

    @Test
    fun extractDocumentSourceKeysDiscoversNestedTextBindingsConditionsAndSourceIcons() {
        val doc = CustomWidgetDocument(
            id = "test-doc",
            meta = CustomWidgetMetadata(name = "Weather & Stocks"),
            root = LayoutContainer(
                id = "root",
                children = listOf(
                    TextNode(
                        id = "temp",
                        template = "Now: {source.weather.temp} • {source.weather.condition}",
                        bindings = mapOf("colorHex" to "{source.weather.color}")
                    ),
                    ImageNode(
                        id = "icon",
                        source = ImageSource.SourceIcon(sourceId = "weather"),
                        showIf = NodeCondition.NotBlank("{source.stocks.ticker}")
                    )
                )
            )
        )

        val keys = PreviewPlaceholderConfig.extractDocumentSourceKeys(doc)
        assertEquals(
            listOf(
                "weather" to "color",
                "weather" to "temp",
                "weather" to "condition",
                "stocks" to "ticker",
                "weather" to "icon"
            ),
            keys
        )
    }
}
