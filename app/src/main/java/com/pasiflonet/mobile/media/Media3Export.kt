package com.pasiflonet.mobile.media

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File

/**
 * Termux/CI-safe fallback:
 * - לא משתמש ב-Media3 Transformer בכלל (כי ה-API משתנה + ב-Termux יש בעיות host tools).
 * - כרגע פשוט מעתיק את הוידאו לקובץ יעד ומחזיר true/false.
 *
 * שים לב: זה "fallback בסיסי". ברגע שתעבוד רק ב-CI/מחשב אפשר להחזיר טרנספורמר אמיתי.
 */
object Media3Export {

    fun exportVideoWithWatermark(
        context: Context,
        inputPath: String,
        outputPath: String,
        watermarkBitmap: Bitmap?,
        opacityPercent: Int,
        scalePercent: Int,
        xPercent: Float,
        yPercent: Float
    ): Boolean {
        return try {
            val src = File(inputPath)
            if (!src.exists()) {
                Log.e("Media3Export", "Input not found: $inputPath")
                return false
            }
            val dst = File(outputPath)
            dst.parentFile?.mkdirs()

            src.inputStream().use { inp ->
                dst.outputStream().use { out ->
                    inp.copyTo(out)
                }
            }
            true
        } catch (t: Throwable) {
            Log.e("Media3Export", "exportVideoWithWatermark failed", t)
            false
        }
    }
}
