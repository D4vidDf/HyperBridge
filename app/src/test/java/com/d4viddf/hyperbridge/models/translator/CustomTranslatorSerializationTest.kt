package com.d4viddf.hyperbridge.models.translator

import com.d4viddf.hyperbridge.data.db.TranslatorEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun testTranslatorRegistryPriorityAndScopeMatching() {
        val mockDao = object : com.d4viddf.hyperbridge.data.db.TranslatorDao {
            override fun getAllTranslatorsFlow() = kotlinx.coroutines.flow.emptyFlow<List<TranslatorEntity>>()
            override fun getActiveTranslatorsFlow() = kotlinx.coroutines.flow.emptyFlow<List<TranslatorEntity>>()
            override suspend fun getActiveTranslators() = emptyList<TranslatorEntity>()
            override suspend fun getTranslatorById(id: String) = null
            override fun getTranslatorByIdFlow(id: String) = kotlinx.coroutines.flow.emptyFlow<TranslatorEntity?>()
            override suspend fun insertTranslator(translator: TranslatorEntity) {}
            override suspend fun insertAll(translators: List<TranslatorEntity>) {}
            override suspend fun updateTranslator(translator: TranslatorEntity) {}
            override suspend fun setTranslatorEnabled(id: String, isEnabled: Boolean, timestamp: Long) {}
            override suspend fun updatePriority(id: String, priority: Int, timestamp: Long) {}
            override suspend fun deleteTranslatorById(id: String) {}
            override suspend fun deleteAll() {}
        }
        val registry = com.d4viddf.hyperbridge.service.translators.TranslatorRegistry(mockDao)

        val globalTranslator = CustomTranslator(
            id = "global.rule",
            meta = TranslatorMetadata(name = "Global Rule"),
            targetScope = TargetScope.GLOBAL,
            priority = 10,
            isEnabled = true
        )

        val appTranslatorLowPriority = CustomTranslator(
            id = "app.spotify.low",
            meta = TranslatorMetadata(name = "Spotify Low"),
            targetScope = TargetScope.SPECIFIC_APPS,
            targetPackages = listOf("com.spotify.music"),
            priority = 50,
            isEnabled = true
        )

        val appTranslatorHighPriority = CustomTranslator(
            id = "app.spotify.high",
            meta = TranslatorMetadata(name = "Spotify High"),
            targetScope = TargetScope.SPECIFIC_APPS,
            targetPackages = listOf("com.spotify.music"),
            priority = 200,
            isEnabled = true
        )

        registry.setTranslatorsForTesting(listOf(globalTranslator, appTranslatorLowPriority, appTranslatorHighPriority))

        // When evaluating for Spotify, the highest priority matching translator should be chosen
        val matchedSpotify = registry.findMatchingTranslator(
            packageName = "com.spotify.music",
            notificationCategory = "transport",
            title = "Song Title",
            text = "Artist Name",
            subtext = null,
            channelId = null,
            extrasKeys = emptySet(),
            hasActions = true,
            hasProgress = false
        )
        assertNotNull(matchedSpotify)
        assertEquals("app.spotify.high", matchedSpotify?.id)

        // When evaluating for another app, it should fall back to the global translator
        val matchedOtherApp = registry.findMatchingTranslator(
            packageName = "com.whatsapp",
            notificationCategory = "msg",
            title = "Alice",
            text = "Hello",
            subtext = null,
            channelId = null,
            extrasKeys = emptySet(),
            hasActions = true,
            hasProgress = false
        )
        assertNotNull(matchedOtherApp)
        assertEquals("global.rule", matchedOtherApp?.id)
    }

    @Test
    fun testTypeSpecificConditionsMatching() {
        val mockDao = object : com.d4viddf.hyperbridge.data.db.TranslatorDao {
            override fun getAllTranslatorsFlow() = kotlinx.coroutines.flow.emptyFlow<List<TranslatorEntity>>()
            override fun getActiveTranslatorsFlow() = kotlinx.coroutines.flow.emptyFlow<List<TranslatorEntity>>()
            override suspend fun getActiveTranslators() = emptyList<TranslatorEntity>()
            override suspend fun getTranslatorById(id: String) = null
            override fun getTranslatorByIdFlow(id: String) = kotlinx.coroutines.flow.emptyFlow<TranslatorEntity?>()
            override suspend fun insertTranslator(translator: TranslatorEntity) {}
            override suspend fun insertAll(translators: List<TranslatorEntity>) {}
            override suspend fun updateTranslator(translator: TranslatorEntity) {}
            override suspend fun setTranslatorEnabled(id: String, isEnabled: Boolean, timestamp: Long) {}
            override suspend fun updatePriority(id: String, priority: Int, timestamp: Long) {}
            override suspend fun deleteTranslatorById(id: String) {}
            override suspend fun deleteAll() {}
        }
        val registry = com.d4viddf.hyperbridge.service.translators.TranslatorRegistry(mockDao)
        val vipMessageTranslator = CustomTranslator(
            id = "msg.vip",
            meta = TranslatorMetadata(name = "VIP Sender"),
            targetScope = TargetScope.GLOBAL,
            priority = 100,
            conditions = TranslatorConditions(
                typeSpecificConditions = TypeSpecificConditions(
                    messaging = MessagingConditions(
                        senderNameRegex = "(?i)Boss|Mom",
                        isGroupConversation = false
                    )
                )
            )
        )

        val mediaPlayingTranslator = CustomTranslator(
            id = "media.playing",
            meta = TranslatorMetadata(name = "Playing Media"),
            targetScope = TargetScope.GLOBAL,
            priority = 90,
            conditions = TranslatorConditions(
                typeSpecificConditions = TypeSpecificConditions(
                    media = MediaConditions(
                        artistRegex = "(?i)Daft Punk",
                        isPlaying = true
                    )
                )
            )
        )

        registry.setTranslatorsForTesting(listOf(vipMessageTranslator, mediaPlayingTranslator))

        // Test VIP message match
        val matchedVip = registry.findMatchingTranslator(
            packageName = "com.whatsapp",
            notificationCategory = "msg",
            title = "Mom",
            text = "Call me when you are home",
            subtext = null,
            channelId = null,
            extrasKeys = emptySet(),
            hasActions = false,
            hasProgress = false,
            senderName = "Mom",
            isGroupConversation = false
        )
        assertNotNull(matchedVip)
        assertEquals("msg.vip", matchedVip?.id)

        // Test non-VIP message should not match
        val matchedNonVip = registry.findMatchingTranslator(
            packageName = "com.whatsapp",
            notificationCategory = "msg",
            title = "John",
            text = "Hey",
            subtext = null,
            channelId = null,
            extrasKeys = emptySet(),
            hasActions = false,
            hasProgress = false,
            senderName = "John",
            isGroupConversation = false
        )
        org.junit.Assert.assertNull(matchedNonVip)

        // Test Media match
        val matchedMedia = registry.findMatchingTranslator(
            packageName = "com.spotify.music",
            notificationCategory = "transport",
            title = "One More Time",
            text = "Discovery",
            subtext = null,
            channelId = null,
            extrasKeys = emptySet(),
            hasActions = true,
            hasProgress = false,
            mediaArtist = "Daft Punk",
            isMediaPlaying = true
        )
        assertNotNull(matchedMedia)
        assertEquals("media.playing", matchedMedia?.id)
    }

    @Test
    fun testProgressTextExtractionRegex() {
        val regexString = "(?<percent>\\d{1,3})%"
        val regex = Regex(regexString)

        val text1 = "Downloading update: 73% completed"
        val match1 = regex.find(text1)
        assertNotNull(match1)
        val extracted1 = match1?.groups?.get("percent")?.value?.toIntOrNull()
        assertEquals(73, extracted1)

        val text2 = "File copy in progress (100%)"
        val match2 = regex.find(text2)
        assertNotNull(match2)
        val extracted2 = match2?.groups?.get("percent")?.value?.toIntOrNull()
        assertEquals(100, extracted2)
    }

    @Test
    fun testOtpExtractionRegex() {
        val regex = Regex("(?i)(?:code|código|otp|passcode|verification|验证码)(?:\\s+(?:is|es))?[:\\s]*([0-9]{4,8})")

        val text1 = "Your verification code is 482910. Do not share it."
        val otp1 = regex.find(text1)?.groupValues?.getOrNull(1)
        assertEquals("482910", otp1)

        val text2 = "Tu código OTP: 9381."
        val otp2 = regex.find(text2)?.groupValues?.getOrNull(1)
        assertEquals("9381", otp2)
    }

    @Test
    fun testHasActiveTranslatorsForPackageAndNotificationTypeScope() {
        val mockDao = object : com.d4viddf.hyperbridge.data.db.TranslatorDao {
            override fun getAllTranslatorsFlow() = kotlinx.coroutines.flow.emptyFlow<List<TranslatorEntity>>()
            override fun getActiveTranslatorsFlow() = kotlinx.coroutines.flow.emptyFlow<List<TranslatorEntity>>()
            override suspend fun getActiveTranslators() = emptyList<TranslatorEntity>()
            override suspend fun getTranslatorById(id: String) = null
            override fun getTranslatorByIdFlow(id: String) = kotlinx.coroutines.flow.emptyFlow<TranslatorEntity?>()
            override suspend fun insertTranslator(translator: TranslatorEntity) {}
            override suspend fun insertAll(translators: List<TranslatorEntity>) {}
            override suspend fun updateTranslator(translator: TranslatorEntity) {}
            override suspend fun setTranslatorEnabled(id: String, isEnabled: Boolean, timestamp: Long) {}
            override suspend fun updatePriority(id: String, priority: Int, timestamp: Long) {}
            override suspend fun deleteTranslatorById(id: String) {}
            override suspend fun deleteAll() {}
        }
        val registry = com.d4viddf.hyperbridge.service.translators.TranslatorRegistry(mockDao)

        val appSpecific = CustomTranslator(
            id = "app.telegram",
            meta = TranslatorMetadata(name = "Telegram Translator"),
            targetScope = TargetScope.SPECIFIC_APPS,
            targetPackages = listOf("org.telegram.messenger"),
            priority = 100,
            isEnabled = true
        )
        registry.setTranslatorsForTesting(listOf(appSpecific))

        assertTrue(registry.hasActiveTranslatorsForPackage("org.telegram.messenger"))
        assertTrue(registry.hasActiveTranslatorsForPackage("ORG.TELEGRAM.MESSENGER"))
        assertFalse(registry.hasActiveTranslatorsForPackage("com.whatsapp"))

        // Add NotificationType scope translator
        val notifTypeTranslator = CustomTranslator(
            id = "type.message",
            meta = TranslatorMetadata(name = "Message Translator"),
            targetScope = TargetScope.NOTIFICATION_TYPE,
            targetNotificationTypes = listOf("MESSAGE"),
            priority = 50,
            isEnabled = true
        )
        registry.setTranslatorsForTesting(listOf(notifTypeTranslator))

        assertTrue(registry.hasActiveTranslatorsForPackage("com.random.app"))

        val matchedType = registry.findMatchingTranslator(
            packageName = "com.random.app",
            notificationCategory = null,
            notificationType = "MESSAGE",
            title = "Friend",
            text = "Hello!",
            subtext = null,
            channelId = null,
            extrasKeys = emptySet(),
            hasActions = false,
            hasProgress = false
        )
        assertNotNull(matchedType)
        assertEquals("type.message", matchedType?.id)

        // Add Global translator (only matches when isLibraryAllowed = true)
        val globalTranslator = CustomTranslator(
            id = "global.catchall",
            meta = TranslatorMetadata(name = "Global Catchall"),
            targetScope = TargetScope.GLOBAL,
            priority = 10,
            isEnabled = true
        )
        registry.setTranslatorsForTesting(listOf(globalTranslator))

        assertTrue(registry.hasActiveTranslatorsForPackage("com.any.unregistered.app", isLibraryAllowed = true))
        assertFalse(registry.hasActiveTranslatorsForPackage("com.any.unregistered.app", isLibraryAllowed = false))

        val matchedGlobalAllowed = registry.findMatchingTranslator(
            packageName = "com.any.unregistered.app",
            notificationCategory = null,
            notificationType = "STANDARD",
            title = "System",
            text = "Alert",
            subtext = null,
            channelId = null,
            extrasKeys = emptySet(),
            hasActions = false,
            hasProgress = false,
            isLibraryAllowed = true
        )
        assertNotNull(matchedGlobalAllowed)
        assertEquals("global.catchall", matchedGlobalAllowed?.id)

        val matchedGlobalDisallowed = registry.findMatchingTranslator(
            packageName = "com.any.unregistered.app",
            notificationCategory = null,
            notificationType = "STANDARD",
            title = "System",
            text = "Alert",
            subtext = null,
            channelId = null,
            extrasKeys = emptySet(),
            hasActions = false,
            hasProgress = false,
            isLibraryAllowed = false
        )
        org.junit.Assert.assertNull(matchedGlobalDisallowed)

        // Add System Apps translator
        val systemAppsTranslator = CustomTranslator(
            id = "system.battery",
            meta = TranslatorMetadata(name = "System Battery Warning"),
            targetScope = TargetScope.SYSTEM_APPS,
            targetPackages = listOf("android", "com.android.systemui"),
            priority = 80,
            isEnabled = true
        )
        registry.setTranslatorsForTesting(listOf(systemAppsTranslator))

        assertTrue(registry.hasActiveTranslatorsForPackage("android", isLibraryAllowed = false))
        assertTrue(registry.hasActiveTranslatorsForPackage("com.android.systemui", isLibraryAllowed = false))
        assertFalse(registry.hasActiveTranslatorsForPackage("com.random.user.app", isLibraryAllowed = false))

        val matchedSystemApp = registry.findMatchingTranslator(
            packageName = "android",
            notificationCategory = "sys",
            notificationType = "STANDARD",
            title = "Low Battery",
            text = "15% remaining",
            subtext = null,
            channelId = null,
            extrasKeys = emptySet(),
            hasActions = false,
            hasProgress = false,
            isLibraryAllowed = false
        )
        assertNotNull(matchedSystemApp)
        assertEquals("system.battery", matchedSystemApp?.id)
    }

    @Test
    fun testSystemAppsScopeSerialization() {
        val translator = CustomTranslator(
            id = "test_system_scope",
            meta = TranslatorMetadata(name = "System Scope Test"),
            targetScope = TargetScope.SYSTEM_APPS,
            targetPackages = listOf("com.miui.securitycenter", "android")
        )
        val json = CustomTranslator.toJson(translator)
        assertTrue(json.contains("\"target_scope\": \"SYSTEM_APPS\""))

        val decoded = CustomTranslator.fromJson(json).getOrThrow()
        assertEquals(TargetScope.SYSTEM_APPS, decoded.targetScope)
        assertEquals(listOf("com.miui.securitycenter", "android"), decoded.targetPackages)
    }
}


