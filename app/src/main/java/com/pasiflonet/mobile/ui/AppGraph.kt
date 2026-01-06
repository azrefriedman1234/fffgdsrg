package com.pasiflonet.mobile.ui

import android.content.Context
import com.pasiflonet.mobile.td.TdRepository

object AppGraph {
    @Volatile private var repo: TdRepository? = null

    fun repo(context: Context): TdRepository {
        return repo ?: synchronized(this) {
            repo ?: TdRepository(context.applicationContext).also { repo = it }
        }
    }
}
