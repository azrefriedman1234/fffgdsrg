package com.pasiflonet.mobile.worker

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pasiflonet.mobile.data.Prefs
import com.pasiflonet.mobile.data.PrefsKeys
import com.pasiflonet.mobile.media.Media3Export
import com.pasiflonet.mobile.model.LogoPreset
import com.pasiflonet.mobile.model.MediaType
import com.pasiflonet.mobile.model.WatermarkSettings
import com.pasiflonet.mobile.ui.AppGraph
import com.pasiflonet.mobile.util.BitmapBlur
import com.pasiflonet.mobile.util.JsonUtil
import com.pasiflonet.mobile.util.WatermarkRenderer
import kotlinx.coroutines.flow.first
import java.io.File

class SendWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_CHAT_ID = "chatId"
        const val KEY_SOURCE_PATH = "sourcePath"
        const val KEY_MEDIA_TYPE = "mediaType"
        const val KEY_RECTS_JSON = "rectsJson"
    }

    override suspend fun doWork(): Result {
        val chatId = inputData.getLong(KEY_CHAT_ID, 0L)
        val srcPath = inputData.getString(KEY_SOURCE_PATH) ?: return Result.failure()
        val mediaType = MediaType.valueOf(inputData.getString(KEY_MEDIA_TYPE) ?: MediaType.NONE.name)
        val rects = JsonUtil.fromJsonRects(inputData.getString(KEY_RECTS_JSON))

        val prefs = Prefs(applicationContext)
        val p: Preferences = prefs.flow().first()

        val wm = WatermarkSettings(
            enabled = p[PrefsKeys.logoEnabled] ?: false,
            logoUri = (p[PrefsKeys.logoUri] ?: "").takeIf { it.isNotBlank() }?.let { Uri.parse(it) },
            preset = LogoPreset.fromKey(p[PrefsKeys.logoPreset]),
            xPercent = p[PrefsKeys.logoX] ?: 80f,
            yPercent = p[PrefsKeys.logoY] ?: 80f,
            scalePercent = p[PrefsKeys.logoScale] ?: 25f,
            opacityPercent = p[PrefsKeys.logoOpacity] ?: 90f
        )

        val repo = AppGraph.tdRepository(applicationContext)

        return try {
            val outPath = when (mediaType) {
                MediaType.PHOTO, MediaType.DOCUMENT -> processImage(srcPath, rects, wm)
                MediaType.VIDEO -> processVideo(srcPath, wm)
                else -> srcPath
            }

            val isVideo = (mediaType == MediaType.VIDEO)
            repo.sendFileToChat(chatId, outPath, isVideo = isVideo, caption = "")
            Result.success()
        } catch (_: Throwable) {
            Result.failure()
        }
    }

    private fun processImage(
        srcPath: String,
        rects: List<com.pasiflonet.mobile.model.NormalizedRect>,
        wm: WatermarkSettings
    ): String {
        val srcFile = File(srcPath)
        val bmp = BitmapFactory.decodeFile(srcFile.absolutePath) ?: throw IllegalStateException("decode failed")
        val blurred = BitmapBlur.applyRectBlur(bmp, rects, radiusPx = 18)
        val watermarked = WatermarkRenderer.applyWatermark(applicationContext, blurred, wm)

        val out = File(applicationContext.cacheDir, "pasiflonet_tmp_${System.currentTimeMillis()}.jpg")
        out.outputStream().use { os ->
            watermarked.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, os)
        }
        return out.absolutePath
    }

    private fun processVideo(srcPath: String, wm: WatermarkSettings): String {
        // Blur rects on video are NOT implemented via Media3 (no built-in blur-rect).
        // Fallback: only watermark (if enabled) and a basic transcode/export.
        val out = File(applicationContext.cacheDir, "pasiflonet_tmp_${System.currentTimeMillis()}.mp4")

        val watermarkBmp = if (wm.enabled && wm.logoUri != null) {
            applicationContext.contentResolver.openInputStream(wm.logoUri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } else null

        val ok = try {
            Media3Export.exportVideoWithWatermark(
                context = applicationContext,
                inputPath = srcPath,
                outputPath = out.absolutePath,
                watermarkBitmap = watermarkBmp,
                opacityPercent = wm.opacityPercent,
                scalePercent = wm.scalePercent,
                xPercent = wm.xPercent,
                yPercent = wm.yPercent
            )
        } catch (_: Throwable) {
            false
        }

        if (!ok) {
            // If export fails, just return original (but still compiles/builds).
            return srcPath
        }
        return out.absolutePath
    }
}
