@file:Suppress("UnstableApiUsage")

package com.pasiflonet.mobile.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.Effects
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Transformer
import androidx.media3.common.util.UnstableApi
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Media3 Transformer export helper.
 *
 * NOTE about blur rectangles on VIDEO:
 * Media3 Transformer provides overlays and some effects, but it doesn't provide a built-in
 * "blur arbitrary rectangles" effect like FFmpeg filtergraph.
 *
 * לכן: בגרסה הזו היישום לוידאו תומך ב-Watermark overlay (BitmapOverlay) ובטרנסקודינג בסיסי.
 *
 * References:
 * - Media3 Transformer "Getting started" dependencies citeturn3search14
 * - BitmapOverlay API citeturn3search11
 */
object Media3Export {

    @UnstableApi
    fun exportVideoWithWatermark(
        context: Context,
        inputPath: String,
        outputPath: String,
        watermarkBitmap: android.graphics.Bitmap?,
        opacityPercent: Float = 90f,
        scalePercent: Float = 25f,
        xPercent: Float = 90f,
        yPercent: Float = 90f
    ): Boolean {
        val inputUri = Uri.fromFile(File(inputPath))
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        val editedBuilder = EditedMediaItem.Builder(MediaItem.fromUri(inputUri))

        if (watermarkBitmap != null) {
            val alpha = (opacityPercent.coerceIn(0f, 100f) / 100f)
            val settings = OverlaySettings.Builder()
                // OverlaySettings uses NDC (-1..+1) anchors; easiest is to use "video frame anchor" and "overlay anchor".
                // We'll keep defaults and rely on the overlay bitmap itself being pre-scaled by the app for now.
                .setAlpha(alpha)
                .build()

            val overlay = BitmapOverlay.createStaticBitmapOverlay(watermarkBitmap, settings)
            val effects = Effects(
                /* audioProcessors= */ emptyList(),
                /* videoEffects= */ listOf(OverlayEffect(listOf(overlay)))
            )
            editedBuilder.setEffects(effects)
        }

        val edited = editedBuilder.build()

        val latch = CountDownLatch(1)
        var success = false

        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: androidx.media3.transformer.Composition, result: Transformer.ExportResult) {
                    success = true
                    latch.countDown()
                }

                override fun onError(composition: androidx.media3.transformer.Composition, result: Transformer.ExportResult, exception: Transformer.ExportException) {
                    success = false
                    latch.countDown()
                }
            })
            .build()

        transformer.start(edited, outputFile.absolutePath)

        latch.await(10, TimeUnit.MINUTES)
        return success && outputFile.exists()
    }
}
