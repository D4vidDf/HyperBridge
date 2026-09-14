package com.d4viddf.hyperbridge.models

data class ActiveIsland(
    val id: Int,
    val type: NotificationType,
    val postTime: Long,
    val sourcePostTime: Long = postTime,
    val packageName: String,
    val sourceKey: String = "",
    val logicalId: String = sourceKey,
    val groupKey: String? = null,
    val isGroupSummary: Boolean = false,
    /** Monotonically increases for each posted content generation of this logical Island. */
    val generation: Long = 0L,
    // Content Diffing Fields
    val title: String,
    val text: String,
    val subText: String,
    // Used for Deduplication
    val lastContentHash: Int,
    /** Distinguishes repeated, semantically identical message events. */
    val messageEventFingerprint: MessageEventFingerprint? = null,
    val callSession: com.d4viddf.hyperbridge.service.call.CallSession? = null,
    val screenRecordingSession: com.d4viddf.hyperbridge.service.recording.ScreenRecordingSession? = null,
    val deleteIntent: android.app.PendingIntent? = null,
    /** The mirrored source should be retired when this bridge notification is opened. */
    val dismissSourceOnContentClick: Boolean = false
)