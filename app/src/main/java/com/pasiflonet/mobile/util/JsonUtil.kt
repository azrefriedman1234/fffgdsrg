package com.pasiflonet.mobile.util

import com.pasiflonet.mobile.model.NormalizedRect
import org.json.JSONArray
import org.json.JSONObject

object JsonUtil {
    fun toJsonRects(rects: List<NormalizedRect>): String {
        val arr = JSONArray()
        rects.forEach { r ->
            arr.put(JSONObject().apply {
                put("x", r.x)
                put("y", r.y)
                put("w", r.w)
                put("h", r.h)
            })
        }
        return arr.toString()
    }

    fun fromJsonRects(json: String?): List<NormalizedRect> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                NormalizedRect(
                    x = o.getDouble("x").toFloat(),
                    y = o.getDouble("y").toFloat(),
                    w = o.getDouble("w").toFloat(),
                    h = o.getDouble("h").toFloat()
                )
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
