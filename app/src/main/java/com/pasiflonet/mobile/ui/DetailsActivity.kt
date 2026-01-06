package com.pasiflonet.mobile.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.pasiflonet.mobile.databinding.ActivityDetailsBinding
import com.pasiflonet.mobile.model.MediaType
import com.pasiflonet.mobile.model.NormalizedRect
import com.pasiflonet.mobile.util.JsonUtil
import com.pasiflonet.mobile.worker.SendWorker
import kotlinx.coroutines.launch
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import coil.load
import java.io.File

class DetailsActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CHAT_ID = "chatId"
        const val EXTRA_FILE_ID = "fileId"
        const val EXTRA_MEDIA_TYPE = "mediaType"
        const val EXTRA_FILE_NAME = "fileName"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_SIZE = "size"
    }

    private lateinit var b: ActivityDetailsBinding
    private val repo by lazy { AppGraph.tdRepository(this) }
    private var player: ExoPlayer? = null

    private var chatId: Long = 0L
    private var fileId: Int = 0
    private var mediaType: MediaType = MediaType.NONE
    private var fileName: String = ""
    private var duration: Int = 0
    private var size: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(b.root)

        chatId = intent.getLongExtra(EXTRA_CHAT_ID, 0L)
        fileId = intent.getIntExtra(EXTRA_FILE_ID, 0)
        mediaType = MediaType.valueOf(intent.getStringExtra(EXTRA_MEDIA_TYPE) ?: MediaType.NONE.name)
        fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: ""
        duration = intent.getIntExtra(EXTRA_DURATION, 0)
        size = intent.getLongExtra(EXTRA_SIZE, 0L)

        b.toolbar.setNavigationOnClickListener { finish() }

        b.tvInfo.text = buildString {
            append("שם קובץ: ").append(fileName.ifBlank { "(לא ידוע)" }).append('\n')
            append("סוג: ").append(mediaType.name).append('\n')
            append("גודל: ").append(size).append(" bytes").append('\n')
            if (duration > 0) append("משך: ").append(duration).append(" שניות").append('\n')
            append("fileId: ").append(fileId)
        }

        // Overlay modes
        b.btnModeAdd.setOnClickListener { b.overlayView.setMode(OverlayView.Mode.ADD) }
        b.btnModeErase.setOnClickListener { b.overlayView.setMode(OverlayView.Mode.ERASE) }
        b.btnUndo.setOnClickListener { b.overlayView.undo() }
        b.btnClearAll.setOnClickListener { b.overlayView.clearAll() }

        b.btnClearTemp.setOnClickListener {
            cacheDir.listFiles()?.forEach { if (it.name.startsWith("pasiflonet_tmp_")) it.delete() }
            Toast.makeText(this, "נוקה.", Toast.LENGTH_SHORT).show()
        }

        b.btnSend.setOnClickListener { enqueueSend() }

        // Download file and preview
        repo.requestFileDownload(fileId)
        repo.getFileLocalPath(fileId) { path ->
            if (path == null) return@getFileLocalPath
            runOnUiThread { showPreview(path) }
        }
    }

    private fun showPreview(localPath: String) {
        val f = File(localPath)
        val isVideo = (mediaType == MediaType.VIDEO)
        if (!isVideo) {
            b.playerView.visibility = View.GONE
            b.ivPreview.visibility = View.VISIBLE
            b.ivPreview.load(f)
        } else {
            b.ivPreview.visibility = View.GONE
            b.playerView.visibility = View.VISIBLE
            player = ExoPlayer.Builder(this).build().also { p ->
                b.playerView.player = p
                p.setMediaItem(MediaItem.fromUri(Uri.fromFile(f)))
                p.prepare()
                p.playWhenReady = false
            }
        }
    }

    private fun enqueueSend() {
        repo.getFileLocalPath(fileId) { path ->
            if (path == null) return@getFileLocalPath
            val rects: List<NormalizedRect> = b.overlayView.getRectsNormalized()
            val input = Data.Builder()
                .putLong(SendWorker.KEY_CHAT_ID, chatId)
                .putString(SendWorker.KEY_SOURCE_PATH, path)
                .putString(SendWorker.KEY_MEDIA_TYPE, mediaType.name)
                .putString(SendWorker.KEY_RECTS_JSON, JsonUtil.toJsonRects(rects))
                .build()

            val req = OneTimeWorkRequestBuilder<SendWorker>()
                .setInputData(input)
                .build()

            b.progress.visibility = View.VISIBLE
            WorkManager.getInstance(this).enqueue(req)

            WorkManager.getInstance(this).getWorkInfoByIdLiveData(req.id).observe(this) { info ->
                if (info == null) return@observe
                if (info.state == WorkInfo.State.SUCCEEDED) {
                    b.progress.visibility = View.GONE
                    Toast.makeText(this, "נשלח ✅", Toast.LENGTH_SHORT).show()
                } else if (info.state == WorkInfo.State.FAILED) {
                    b.progress.visibility = View.GONE
                    Toast.makeText(this, "שליחה נכשלה ❌", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }
}
