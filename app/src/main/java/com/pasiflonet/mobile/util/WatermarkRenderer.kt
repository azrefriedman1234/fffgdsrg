package com.pasiflonet.mobile.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import com.pasiflonet.mobile.model.LogoPreset
import com.pasiflonet.mobile.model.WatermarkSettings
import kotlin.math.max
import kotlin.math.min

object WatermarkRenderer {

    fun applyWatermark(
        context: Context,
        source: Bitmap,
        settings: WatermarkSettings
    ): Bitmap {
        if (!settings.enabled) return source
        val logoUri = settings.logoUri ?: return source

        val logoBitmap = loadBitmap(context, logoUri) ?: return source
        val out = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = ((settings.opacityPercent.coerceIn(0f, 100f) / 100f) * 255f).toInt()
            isFilterBitmap = true
        }

        val baseW = out.width.toFloat()
        val baseH = out.height.toFloat()

        val scale = (settings.scalePercent.coerceIn(1f, 200f) / 100f)
        val targetW = max(16f, baseW * 0.25f * scale)
        val ratio = logoBitmap.height.toFloat() / max(1f, logoBitmap.width.toFloat())
        val targetH = targetW * ratio

        val pos = computePosition(
            preset = settings.preset,
            xPercent = settings.xPercent,
            yPercent = settings.yPercent,
            baseW = baseW,
            baseH = baseH,
            wmW = targetW,
            wmH = targetH
        )

        val dst = RectF(pos.first, pos.second, pos.first + targetW, pos.second + targetH)
        canvas.drawBitmap(logoBitmap, null, dst, paint)
        return out
    }

    private fun computePosition(
        preset: LogoPreset,
        xPercent: Float,
        yPercent: Float,
        baseW: Float,
        baseH: Float,
        wmW: Float,
        wmH: Float
    ): Pair<Float, Float> {
        val margin = min(baseW, baseH) * 0.02f

        return when (preset) {
            LogoPreset.TOP_LEFT -> margin to margin
            LogoPreset.TOP_RIGHT -> (baseW - wmW - margin) to margin
            LogoPreset.BOTTOM_LEFT -> margin to (baseH - wmH - margin)
            LogoPreset.BOTTOM_RIGHT -> (baseW - wmW - margin) to (baseH - wmH - margin)
            LogoPreset.CENTER -> ((baseW - wmW) / 2f) to ((baseH - wmH) / 2f)
            else -> {
                val x = (xPercent.coerceIn(0f, 100f) / 100f) * baseW - wmW / 2f
                val y = (yPercent.coerceIn(0f, 100f) / 100f) * baseH - wmH / 2f
                x.coerceIn(0f, baseW - wmW) to y.coerceIn(0f, baseH - wmH)
            }
        }
    }

    private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                android.graphics.BitmapFactory.decodeStream(input)
            }
        } catch (_: Throwable) {
            null
        }
    }
}
