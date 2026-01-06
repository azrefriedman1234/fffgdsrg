package com.pasiflonet.mobile.ui

import android.content.Context
import com.pasiflonet.mobile.data.AppPrefs
import com.pasiflonet.mobile.td.TdRepository

class AppGraph(ctx: Context) {
    val prefs = AppPrefs(ctx.applicationContext)
    val tdRepository = TdRepository(ctx.applicationContext, prefs)
}
