package com.pasiflonet.mobile.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.pasiflonet.mobile.model.NormalizedRect
import kotlin.math.max
import kotlin.math.min

/**
 * Simple (CPU) box blur per-rectangle.
 * This is not the fastest, but it is stable and does not require RenderScript/FFmpeg.
 */
object BitmapBlur {

    fun applyRectBlur(
        source: Bitmap,
        rects: List<NormalizedRect>,
        radiusPx: Int = 18
    ): Bitmap {
        val out = source.copy(Bitmap.Config.ARGB_8888, true)
        val w = out.width
        val h = out.height
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        rects.forEach { nr ->
            val r = Rect(
                (nr.x * w).toInt().coerceIn(0, w - 1),
                (nr.y * h).toInt().coerceIn(0, h - 1),
                ((nr.x + nr.w) * w).toInt().coerceIn(0, w),
                ((nr.y + nr.h) * h).toInt().coerceIn(0, h)
            )
            if (r.width() < 2 || r.height() < 2) return@forEach

            val crop = Bitmap.createBitmap(out, r.left, r.top, r.width(), r.height())
            val blurred = fastBoxBlur(crop, radiusPx)
            canvas.drawBitmap(blurred, r.left.toFloat(), r.top.toFloat(), paint)
            crop.recycle()
            blurred.recycle()
        }
        return out
    }

    // A straightforward, stack-ish box blur (two-pass). Radius must be >=1.
    private fun fastBoxBlur(src: Bitmap, radius: Int): Bitmap {
        val r = max(1, min(64, radius))
        val w = src.width
        val h = src.height
        val pix = IntArray(w * h)
        src.getPixels(pix, 0, w, 0, 0, w, h)

        val tmp = IntArray(w * h)

        // Horizontal
        for (y in 0 until h) {
            var rsum = 0
            var gsum = 0
            var bsum = 0
            var asum = 0
            val yi = y * w
            for (i in -r..r) {
                val x = i.coerceIn(0, w - 1)
                val c = pix[yi + x]
                asum += (c ushr 24) and 0xff
                rsum += (c ushr 16) and 0xff
                gsum += (c ushr 8) and 0xff
                bsum += c and 0xff
            }
            for (x in 0 until w) {
                tmp[yi + x] = ((asum / (2 * r + 1)) shl 24) or
                    ((rsum / (2 * r + 1)) shl 16) or
                    ((gsum / (2 * r + 1)) shl 8) or
                    (bsum / (2 * r + 1))

                val x1 = (x - r).coerceIn(0, w - 1)
                val x2 = (x + r + 1).coerceIn(0, w - 1)
                val c1 = pix[yi + x1]
                val c2 = pix[yi + x2]
                asum += ((c2 ushr 24) and 0xff) - ((c1 ushr 24) and 0xff)
                rsum += ((c2 ushr 16) and 0xff) - ((c1 ushr 16) and 0xff)
                gsum += ((c2 ushr 8) and 0xff) - ((c1 ushr 8) and 0xff)
                bsum += (c2 and 0xff) - (c1 and 0xff)
            }
        }

        // Vertical
        val out = IntArray(w * h)
        for (x in 0 until w) {
            var rsum = 0
            var gsum = 0
            var bsum = 0
            var asum = 0
            for (i in -r..r) {
                val y = i.coerceIn(0, h - 1)
                val c = tmp[y * w + x]
                asum += (c ushr 24) and 0xff
                rsum += (c ushr 16) and 0xff
                gsum += (c ushr 8) and 0xff
                bsum += c and 0xff
            }
            for (y in 0 until h) {
                out[y * w + x] = ((asum / (2 * r + 1)) shl 24) or
                    ((rsum / (2 * r + 1)) shl 16) or
                    ((gsum / (2 * r + 1)) shl 8) or
                    (bsum / (2 * r + 1))

                val y1 = (y - r).coerceIn(0, h - 1)
                val y2 = (y + r + 1).coerceIn(0, h - 1)
                val c1 = tmp[y1 * w + x]
                val c2 = tmp[y2 * w + x]
                asum += ((c2 ushr 24) and 0xff) - ((c1 ushr 24) and 0xff)
                rsum += ((c2 ushr 16) and 0xff) - ((c1 ushr 16) and 0xff)
                gsum += ((c2 ushr 8) and 0xff) - ((c1 ushr 8) and 0xff)
                bsum += (c2 and 0xff) - (c1 and 0xff)
            }
        }

        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }
}
