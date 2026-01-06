package com.pasiflonet.mobile.ui

import android.content.Context
import com.pasiflonet.mobile.data.AppPrefs
import com.pasiflonet.mobile.td.TdRepository

object AppGraph {
    @Volatile private var prefsInst: AppPrefs? = null
    @Volatile private var repoInst: TdRepository? = null

    fun prefs(ctx: Context): AppPrefs {
        return prefsInst ?: synchronized(this) {
            prefsInst ?: AppPrefs(ctx.applicationContext).also { prefsInst = it }
        }
    }

    fun tdRepository(ctx: Context): TdRepository {
        return repoInst ?: synchronized(this) {
            repoInst ?: TdRepository(ctx.applicationContext, prefs(ctx)).also { repoInst = it }
        }
    }
}
