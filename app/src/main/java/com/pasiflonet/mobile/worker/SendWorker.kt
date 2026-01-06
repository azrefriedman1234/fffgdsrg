package com.pasiflonet.mobile.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pasiflonet.mobile.ui.AppGraph

class SendWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val chatId = inputData.getLong("chatId", 0L)
        val path = inputData.getString("path") ?: return Result.failure()
        val caption = inputData.getString("caption")

        val repo = AppGraph.repo(applicationContext)

        var ok = false
        repo.sendFileToChat(chatId, path, caption) { success ->
            ok = success
        }

        return if (ok) Result.success() else Result.failure()
    }
}
