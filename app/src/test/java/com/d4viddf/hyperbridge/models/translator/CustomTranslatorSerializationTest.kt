package com.d4viddf.hyperbridge.models.translator

import com.d4viddf.hyperbridge.data.db.TranslatorEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomTranslatorSerializationTest {

    @Test
    fun testDefaultSerializationRoundtrip() {
        val translator = CustomTranslator(
            id = "com.spotify.lyrics_island",
            meta = TranslatorMetadata(
                name = "Spotify Dynamic Now Playing",
                author = "Community",
                version = 1,
                description = "Enhanced Spotify Island with lyrics and custom actions."
            ),
            targetScope = TargetScope.SPECIFIC_APPS,
            targetPackages = listOf("com.spotify.music", "com.spotify.lite"),
            targetNotificationTypes = listOf("MEDIA"),
            priority = 250,
            isEnabled = true,
            conditions = TranslatorConditions(
                titleRegex = ".*",
                textRegex = ".*",
                category = "transport",
                hasExtras = listOf("android.mediaSession"),
                typeSpecificConditions = TypeSpecificConditions(
                    media = MediaConditions(
                        artistRegex = "(?i)Daft Punk|Coldplay",
                        isPlaying = true
                    ),
                    messaging = MessagingConditions(
                        senderNameRegex = "(?i)VIP.*",
                        isGroupConversation = false
                    )
                )
            ),
            themeBinding = ThemeBinding(
                themeId = "cyberpunk_neon",
                fallbackToActiveIfMissing = true,
                iconShapeId = "squircle",
                iconPaddingPercent = 18
            ),
            engineMode = EngineMode.CUSTOM_ISLAND,
            behaviorOverride = BehaviorOverride(
                engineMode = EngineMode.CUSTOM_ISLAND,
                isFloat = true,
                floatTimeoutSeconds = 6,
                timeoutSeconds = 45,
                isShowShade = true
            ),
            dataExtraction = DataExtractionConfig(
                extractProgressFromText = true,
                progressRegex = "(?<percent>\\d{1,3})%",
                customMaxProgress = 100,
                substituteVariables = mapOf("artist" to "{notif.media_artist}")
            ),
            presentation = PresentationConfig(
                mode = PresentationMode.TEMPLATE,
                templateId = "TEMPLATE_3",
                leftSlot = SlotConfig(
                    source = "ALBUM_ART_OR_APP_ICON",
                    shape = "squircle"
                ),
                textSlot = TextSlotConfig(
                    titleTemplate = "{notif.title}",
                    subtitleTemplate = "{notif.text} • {notif.subtext}"
                ),
                progressSlot = ProgressSlotConfig(
                    type = ProgressSlotType.PROGRESS_BAR,
                    progressSource = "AUTO_DETECT",
                    showPercentage = true
                ),
                actionSlots = listOf(
                    ActionSlotConfig(
                        slotPosition = 0,
                        isVisible = true,
                        source = ActionSource.NOTIFICATION_ACTION,
                        actionMatcher = ActionMatcher(
                            matchBy = ActionMatchBy.INDEX,
                            actionIndex = 1
                        ),
                        displayMode = ActionDisplayMode.ICON_ONLY
                    ),
                    ActionSlotConfig(
                        slotPosition = 1,
                        isVisible = true,
                        source = ActionSource.SMART_ACTION,
                        smartActionType = SmartActionType.OTP_COPY,
                        actionMatcher = ActionMatcher(
                            matchBy = ActionMatchBy.TITLE,
                            titleRegex = "(?i)copy|code"
                        ),
                        displayMode = ActionDisplayMode.ICON_AND_TEXT,
                        customLabel = "Copy OTP"
                    )
                )
            )
        )

        val jsonString = CustomTranslator.toJson(translator)
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("com.spotify.lyrics_island"))
        assertTrue(jsonString.contains("OTP_COPY"))
        assertTrue(jsonString.contains("action_index"))

        val decodedResult = CustomTranslator.fromJson(jsonString)
        assertTrue(decodedResult.isSuccess)
        val decoded = decodedResult.getOrThrow()

        assertEquals(translator.id, decoded.id)
        assertEquals(translator.meta.name, decoded.meta.name)
        assertEquals(TargetScope.SPECIFIC_APPS, decoded.targetScope)
        assertEquals(2, decoded.targetPackages.size)
        assertEquals(250, decoded.priority)
        assertEquals(EngineMode.CUSTOM_ISLAND, decoded.engineMode)
        assertEquals(true, decoded.dataExtraction.extractProgressFromText)
        assertEquals(2, decoded.presentation.actionSlots.size)
        assertEquals(ActionMatchBy.INDEX, decoded.presentation.actionSlots[0].actionMatcher.matchBy)
        assertEquals(1, decoded.presentation.actionSlots[0].actionMatcher.actionIndex)
        assertEquals(SmartActionType.OTP_COPY, decoded.presentation.actionSlots[1].smartActionType)
    }

    @Test
    fun testRoomEntityConversionRoundtrip() {
        val translator = CustomTranslator(
            id = "global.otp.copy",
            meta = TranslatorMetadata(
                name = "Global OTP One-Tap Copy",
                author = "HyperBridge Team"
            ),
            targetScope = TargetScope.GLOBAL,
            priority = 500,
            isEnabled = true
        )

        val entity = TranslatorEntity.fromCustomTranslator(translator)
        assertEquals("global.otp.copy", entity.id)
        assertEquals("GLOBAL", entity.targetScope)
        assertEquals(500, entity.priority)
        assertTrue(entity.isEnabled)

        val convertedBack = entity.toCustomTranslator().getOrThrow()
        assertEquals(translator.id, convertedBack.id)
        assertEquals(TargetScope.GLOBAL, convertedBack.targetScope)
        assertEquals(500, convertedBack.priority)
    }

    @Test
    fun testBackwardCompatibilityWithMinimalJson() {
        val minimalJson = """
            {
                "id": "minimal.translator",
                "meta": {
                    "name": "Minimal"
                }
            }
        """.trimIndent()

        val result = CustomTranslator.fromJson(minimalJson)
        assertTrue(result.isSuccess)
        val decoded = result.getOrThrow()

        assertEquals("minimal.translator", decoded.id)
        assertEquals("Minimal", decoded.meta.name)
        assertEquals(TargetScope.SPECIFIC_APPS, decoded.targetScope)
        assertEquals(EngineMode.INHERIT, decoded.engineMode)
        assertEquals(100, decoded.priority)
        assertEquals(true, decoded.isEnabled)
        assertEquals(true, decoded.themeBinding.fallbackToActiveIfMissing)
        assertEquals("active", decoded.themeBinding.themeId)
    }
}
