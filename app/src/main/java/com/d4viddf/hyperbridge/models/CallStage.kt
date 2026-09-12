package com.d4viddf.hyperbridge.models

import androidx.annotation.StringRes
import com.d4viddf.hyperbridge.R

/** User-selectable voice-call lifecycle groups. Existing installs default to every stage. */
enum class CallStage(
    @param:StringRes val labelRes: Int,
    @param:StringRes val descriptionRes: Int
) {
    INCOMING(R.string.call_stage_incoming, R.string.call_stage_incoming_desc),
    OUTGOING(R.string.call_stage_outgoing, R.string.call_stage_outgoing_desc),
    ACTIVE(R.string.call_stage_active, R.string.call_stage_active_desc)
}
