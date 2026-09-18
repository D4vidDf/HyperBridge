package com.d4viddf.hyperbridge.service.translators

import com.d4viddf.hyperbridge.data.db.TranslatorDao
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.TargetScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe in-memory cache and registry for Custom Translators (.htrans).
 * Updates automatically when the underlying Room database changes.
 */
class TranslatorRegistry(
    private val translatorDao: TranslatorDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val activeTranslators = CopyOnWriteArrayList<CustomTranslator>()

    init {
        scope.launch {
            translatorDao.getActiveTranslatorsFlow().collectLatest { entities ->
                val parsed = entities.mapNotNull { entity ->
                    entity.toCustomTranslator().getOrNull()
                }.sortedByDescending { it.priority }
                activeTranslators.clear()
                activeTranslators.addAll(parsed)
            }
        }
    }

    /**
     * Finds the highest-priority matching Custom Translator for a given notification.
     * Evaluates App-Specific translators first, then Notification-Type translators, then Global translators.
     */
    fun findMatchingTranslator(
        packageName: String,
        notificationCategory: String?,
        title: String,
        text: String,
        subtext: String?,
        channelId: String?,
        extrasKeys: Set<String>,
        hasActions: Boolean,
        hasProgress: Boolean,
        senderName: String? = null,
        conversationTitle: String? = null,
        isGroupConversation: Boolean? = null,
        mediaArtist: String? = null,
        mediaAlbum: String? = null,
        isMediaPlaying: Boolean? = null,
        callerName: String? = null,
        progressPercent: Int? = null
    ): CustomTranslator? {
        val candidates = activeTranslators.filter { it.isEnabled }

        for (translator in candidates) {
            if (!matchesScope(translator, packageName, notificationCategory)) {
                continue
            }
            if (!matchesConditions(
                    translator = translator,
                    title = title,
                    text = text,
                    subtext = subtext,
                    category = notificationCategory,
                    channelId = channelId,
                    extrasKeys = extrasKeys,
                    hasActions = hasActions,
                    hasProgress = hasProgress,
                    senderName = senderName,
                    conversationTitle = conversationTitle,
                    isGroupConversation = isGroupConversation,
                    mediaArtist = mediaArtist,
                    mediaAlbum = mediaAlbum,
                    isMediaPlaying = isMediaPlaying,
                    callerName = callerName,
                    progressPercent = progressPercent
                )
            ) {
                continue
            }
            return translator
        }
        return null
    }

    private fun matchesScope(
        translator: CustomTranslator,
        packageName: String,
        notificationCategory: String?
    ): Boolean {
        return when (translator.targetScope) {
            TargetScope.GLOBAL -> true
            TargetScope.SPECIFIC_APPS -> {
                translator.targetPackages.any { it.equals(packageName, ignoreCase = true) }
            }
            TargetScope.NOTIFICATION_TYPE -> {
                if (translator.targetNotificationTypes.isEmpty()) return true
                if (notificationCategory == null) return false
                translator.targetNotificationTypes.any { it.equals(notificationCategory, ignoreCase = true) }
            }
        }
    }

    private fun matchesConditions(
        translator: CustomTranslator,
        title: String,
        text: String,
        subtext: String?,
        category: String?,
        channelId: String?,
        extrasKeys: Set<String>,
        hasActions: Boolean,
        hasProgress: Boolean,
        senderName: String?,
        conversationTitle: String?,
        isGroupConversation: Boolean?,
        mediaArtist: String?,
        mediaAlbum: String?,
        isMediaPlaying: Boolean?,
        callerName: String?,
        progressPercent: Int?
    ): Boolean {
        val cond = translator.conditions

        // Title regex
        if (!cond.titleRegex.isNullOrEmpty()) {
            val pattern = Regex(cond.titleRegex)
            if (!pattern.containsMatchIn(title)) return false
        }

        // Text regex
        if (!cond.textRegex.isNullOrEmpty()) {
            val pattern = Regex(cond.textRegex)
            if (!pattern.containsMatchIn(text)) return false
        }

        // Subtext regex
        if (!cond.subtextRegex.isNullOrEmpty()) {
            val st = subtext ?: ""
            val pattern = Regex(cond.subtextRegex)
            if (!pattern.containsMatchIn(st)) return false
        }

        // Category match
        if (!cond.category.isNullOrEmpty()) {
            if (!cond.category.equals(category, ignoreCase = true)) return false
        }

        // Channel ID match
        if (!cond.channelId.isNullOrEmpty()) {
            if (!cond.channelId.equals(channelId, ignoreCase = true)) return false
        }

        // Has extras check
        if (cond.hasExtras.isNotEmpty()) {
            if (!cond.hasExtras.all { extrasKeys.contains(it) }) return false
        }

        // Has actions check
        if (cond.hasActions != null && cond.hasActions != hasActions) {
            return false
        }

        // Has progress check
        if (cond.hasProgress != null && cond.hasProgress != hasProgress) {
            return false
        }

        // Type-specific conditions
        val typeCond = cond.typeSpecificConditions
        if (typeCond != null) {
            // Messaging conditions
            val msgCond = typeCond.messaging
            if (msgCond != null) {
                if (!msgCond.senderNameRegex.isNullOrEmpty()) {
                    val sName = senderName ?: ""
                    if (!Regex(msgCond.senderNameRegex).containsMatchIn(sName)) return false
                }
                if (!msgCond.conversationTitleRegex.isNullOrEmpty()) {
                    val cTitle = conversationTitle ?: ""
                    if (!Regex(msgCond.conversationTitleRegex).containsMatchIn(cTitle)) return false
                }
                if (msgCond.isGroupConversation != null && msgCond.isGroupConversation != isGroupConversation) {
                    return false
                }
            }

            // Media conditions
            val mediaCond = typeCond.media
            if (mediaCond != null) {
                if (!mediaCond.artistRegex.isNullOrEmpty()) {
                    val artist = mediaArtist ?: ""
                    if (!Regex(mediaCond.artistRegex).containsMatchIn(artist)) return false
                }
                if (!mediaCond.albumRegex.isNullOrEmpty()) {
                    val album = mediaAlbum ?: ""
                    if (!Regex(mediaCond.albumRegex).containsMatchIn(album)) return false
                }
                if (mediaCond.isPlaying != null && mediaCond.isPlaying != isMediaPlaying) {
                    return false
                }
            }

            // Call conditions
            val callCond = typeCond.call
            if (callCond != null) {
                if (!callCond.callerNameRegex.isNullOrEmpty()) {
                    val cName = callerName ?: ""
                    if (!Regex(callCond.callerNameRegex).containsMatchIn(cName)) return false
                }
            }

            // Progress conditions
            val progCond = typeCond.progress
            if (progCond != null && progressPercent != null) {
                if (progCond.minProgressPercent != null && progressPercent < progCond.minProgressPercent) {
                    return false
                }
                if (progCond.maxProgressPercent != null && progressPercent > progCond.maxProgressPercent) {
                    return false
                }
            }
        }

        return true
    }

    /** Manually inject for synchronous testing or offline registry evaluation */
    fun setTranslatorsForTesting(translators: List<CustomTranslator>) {
        activeTranslators.clear()
        activeTranslators.addAll(translators.sortedByDescending { it.priority })
    }
}
