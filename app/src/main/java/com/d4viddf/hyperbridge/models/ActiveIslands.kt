package com.d4viddf.hyperbridge.models

data class ActiveIsland(
    val id: Int,
    val type: NotificationType,
    val postTime: Long,
    val packageName: String,
    // Content Diffing Fields
    val title: String,
    val text: String,
    val subText: String,
    // Used for Deduplication
    val lastContentHash: Int
)