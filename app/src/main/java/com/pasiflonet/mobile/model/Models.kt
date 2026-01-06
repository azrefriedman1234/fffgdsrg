package com.pasiflonet.mobile.model

import android.net.Uri

data class ChatUi(
    val id: Long,
    val title: String,
    val subtitle: String = ""
)

enum class MediaType { NONE, PHOTO, VIDEO, DOCUMENT }

data class MessageUi(
    val chatId: Long,
    val messageId: Long,
    val text: String,
    val mediaType: MediaType,
    val fileId: Int? = null,
    val fileName: String? = null,
    val thumbnailFileId: Int? = null,
    val durationSec: Int? = null,
    val sizeBytes: Long? = null
)

data class NormalizedRect(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float
)

enum class LogoPreset(val key: String) {
    TOP_LEFT("top_left"),
    TOP_RIGHT("top_right"),
    BOTTOM_LEFT("bottom_left"),
    BOTTOM_RIGHT("bottom_right"),
    CENTER("center");

    companion object {
        fun fromKey(key: String?): LogoPreset =
            values().firstOrNull { it.key == key } ?: BOTTOM_RIGHT
    }
}

data class WatermarkSettings(
    val enabled: Boolean,
    val logoUri: Uri?,
    val preset: LogoPreset,
    val xPercent: Float,
    val yPercent: Float,
    val scalePercent: Float,
    val opacityPercent: Float
)
