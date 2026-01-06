package com.pasiflonet.mobile.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.jvm.functions.Function1
import kotlin.jvm.functions.Function2

class SendWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val chatId = inputData.getLong(KEY_CHAT_ID, 0L)
        val filePath = inputData.getString(KEY_FILE_PATH)
        val caption = inputData.getString(KEY_CAPTION) ?: ""

        if (chatId == 0L || filePath.isNullOrBlank()) {
            Log.e(TAG, "Missing input: chatId=$chatId filePath=$filePath")
            return Result.failure()
        }

        return try {
            val ok = sendViaRepository(applicationContext, chatId, filePath, caption)
            if (ok) Result.success() else Result.retry()
        } catch (t: Throwable) {
            Log.e(TAG, "SendWorker failed", t)
            Result.retry()
        }
    }

    private suspend fun sendViaRepository(
        ctx: Context,
        chatId: Long,
        filePath: String,
        caption: String
    ): Boolean {
        val repo = resolveTdRepository(ctx) ?: run {
            Log.e(TAG, "TdRepository not found/constructable")
            return false
        }

        val names = listOf(
            "sendFileToChat",
            "sendMediaToChat",
            "sendToChat",
            "sendFile"
        )

        // 1) (Long, String, String, callback)
        for (name in names) {
            val m = repo.javaClass.methods.firstOrNull { it.name == name && it.parameterTypes.size == 4 }
            if (m != null) {
                val last = m.parameterTypes[3]

                // callback: Function2<Boolean, Any?, Unit>
                if (Function2::class.java.isAssignableFrom(last)) {
                    return suspendCancellableCoroutine { cont ->
                        val cb = Function2<Boolean, Any?, kotlin.Unit> { ok: Boolean, _: Any? ->
                            if (cont.isActive) cont.resume(ok)
                            kotlin.Unit
                        }
                        try {
                            m.invoke(repo, chatId, filePath, caption, cb)
                        } catch (t: Throwable) {
                            Log.e(TAG, "Invoke $name(4) failed", t)
                            if (cont.isActive) cont.resume(false)
                        }
                    }
                }

                // callback: Function1<Any?, Unit>  (נחשב הצלחה אם חזר callback בלי חריגה)
                if (Function1::class.java.isAssignableFrom(last)) {
                    return suspendCancellableCoroutine { cont ->
                        val cb = Function1<Any?, kotlin.Unit> { _: Any? ->
                            if (cont.isActive) cont.resume(true)
                            kotlin.Unit
                        }
                        try {
                            m.invoke(repo, chatId, filePath, caption, cb)
                        } catch (t: Throwable) {
                            Log.e(TAG, "Invoke $name(4) failed", t)
                            if (cont.isActive) cont.resume(false)
                        }
                    }
                }
            }
        }

        // 2) (Long, String, String) -> Boolean/Unit
        for (name in names) {
            val m = repo.javaClass.methods.firstOrNull { it.name == name && it.parameterTypes.size == 3 }
            if (m != null) {
                return try {
                    val res = m.invoke(repo, chatId, filePath, caption)
                    (res as? Boolean) ?: true // אם Unit => true
                } catch (t: Throwable) {
                    Log.e(TAG, "Invoke $name(3) failed", t)
                    false
                }
            }
        }

        // 3) (Long, String) -> Boolean/Unit
        for (name in names) {
            val m = repo.javaClass.methods.firstOrNull { it.name == name && it.parameterTypes.size == 2 }
            if (m != null) {
                return try {
                    val res = m.invoke(repo, chatId, filePath)
                    (res as? Boolean) ?: true
                } catch (t: Throwable) {
                    Log.e(TAG, "Invoke $name(2) failed", t)
                    false
                }
            }
        }

        Log.e(TAG, "No suitable send method found on TdRepository: ${repo.javaClass.name}")
        return false
    }

    private fun resolveTdRepository(ctx: Context): Any? {
        // 1) אם יש AppGraph שמחזיר repo
        runCatching {
            val clazz = Class.forName("com.pasiflonet.mobile.ui.AppGraph")
            val instance = clazz.getDeclaredField("INSTANCE").get(null)
            val methods = clazz.methods

            // מחפשים פונקציה שמחזירה TdRepository (או משהו דומה) ומקבלת Context או ללא פרמטרים
            val m1 = methods.firstOrNull { it.name in listOf("repo", "tdRepository", "getRepo", "getTdRepository") && it.parameterTypes.size == 1 }
            if (m1 != null) return m1.invoke(instance, ctx)

            val m0 = methods.firstOrNull { it.name in listOf("repo", "tdRepository", "getRepo", "getTdRepository") && it.parameterTypes.isEmpty() }
            if (m0 != null) return m0.invoke(instance)
        }

        // 2) ניסוי בנייה ישירה של TdRepository(Context)
        return runCatching {
            val repoClazz = Class.forName("com.pasiflonet.mobile.td.TdRepository")
            val ctor = repoClazz.constructors.firstOrNull { it.parameterTypes.size == 1 && Context::class.java.isAssignableFrom(it.parameterTypes[0]) }
            ctor?.newInstance(ctx)
        }.getOrNull()
    }

    companion object {
        private const val TAG = "SendWorker"

        const val KEY_CHAT_ID = "chat_id"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_CAPTION = "caption"
    }
}
