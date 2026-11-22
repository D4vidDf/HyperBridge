package com.d4viddf.hyperbridge.service.call

import com.d4viddf.hyperbridge.models.NotificationType

/**
 * Some calling apps (Instagram, Messenger) do not remove their call notification when the call
 * ends. They rewrite it in place, on the same key, into a follow-up such as "Missed call" or
 * "Call ended" with Message / Call back actions, which [CallNotificationClassifier] no longer
 * reports as a call.
 *
 * The bridged call Island is keyed by its logical call id, not by the source key, so neither the
 * removal callback (the source is still posted) nor the periodic sync (the source key is still
 * active) retires it. It kept counting the call timer for as long as the follow-up stayed in the
 * shade. A tracked call source that is now classified as anything other than a call ends that
 * call.
 */
object CallFollowUpPolicy {
    fun shouldEndCall(trackedCallId: String?, detectedType: NotificationType): Boolean =
        trackedCallId != null && detectedType != NotificationType.CALL
}
