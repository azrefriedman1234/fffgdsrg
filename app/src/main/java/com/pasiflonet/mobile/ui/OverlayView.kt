package com.pasiflonet.mobile.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.pasiflonet.mobile.model.NormalizedRect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Draws and edits rectangles on top of a preview.
 * Stores rectangles in normalized coordinates (0..1).
 */
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class Mode { ADD, ERASE }
    private var mode: Mode = Mode.ADD

    private val paintRect = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.argb(220, 255, 0, 0)
    }
    private val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(60, 255, 0, 0)
    }
    private val paintHandle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(220, 255, 255, 255)
    }

    private val rects = ArrayList<RectF>()
    private val undoStack = ArrayDeque<List<RectF>>()

    private var activeIndex: Int = -1
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var activeOriginal = RectF()
    private var resizingCorner: Corner? = null

    private enum class Corner { TL, TR, BL, BR }

    fun setMode(m: Mode) {
        mode = m
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        rects.clear()
        rects.addAll(prev.map { RectF(it) })
        invalidate()
    }

    fun clearAll() {
        snapshot()
        rects.clear()
        invalidate()
    }

    fun getRectsNormalized(): List<NormalizedRect> {
        val w = width.toFloat().takeIf { it > 0f } ?: return emptyList()
        val h = height.toFloat().takeIf { it > 0f } ?: return emptyList()
        return rects.map { r ->
            val left = (r.left / w).coerceIn(0f, 1f)
            val top = (r.top / h).coerceIn(0f, 1f)
            val ww = (r.width() / w).coerceIn(0f, 1f)
            val hh = (r.height() / h).coerceIn(0f, 1f)
            NormalizedRect(left, top, ww, hh)
        }
    }

    fun setRectsNormalized(list: List<NormalizedRect>) {
        snapshot()
        rects.clear()
        val w = width.toFloat().takeIf { it > 0f } ?: return
        val h = height.toFloat().takeIf { it > 0f } ?: return
        for (nr in list) {
            rects.add(
                RectF(
                    nr.x * w,
                    nr.y * h,
                    (nr.x + nr.w) * w,
                    (nr.y + nr.h) * h
                )
            )
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        rects.forEachIndexed { idx, r ->
            canvas.drawRect(r, paintFill)
            canvas.drawRect(r, paintRect)
            // handles
            val hs = 14f
            canvas.drawCircle(r.left, r.top, hs, paintHandle)
            canvas.drawCircle(r.right, r.top, hs, paintHandle)
            canvas.drawCircle(r.left, r.bottom, hs, paintHandle)
            canvas.drawCircle(r.right, r.bottom, hs, paintHandle)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activeIndex = hitRectIndex(x, y)
                dragStartX = x
                dragStartY = y
                resizingCorner = activeIndex.takeIf { it >= 0 }?.let { cornerAt(rects[it], x, y) }
                activeOriginal = if (activeIndex >= 0) RectF(rects[activeIndex]) else RectF()

                if (mode == Mode.ERASE && activeIndex >= 0) {
                    snapshot()
                    rects.removeAt(activeIndex)
                    activeIndex = -1
                    invalidate()
                    return true
                }

                if (mode == Mode.ADD && activeIndex == -1) {
                    snapshot()
                    val r = RectF(x, y, x, y)
                    rects.add(r)
                    activeIndex = rects.lastIndex
                    resizingCorner = Corner.BR
                    invalidate()
                    return true
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeIndex < 0) return true
                val r = rects[activeIndex]
                val dx = x - dragStartX
                val dy = y - dragStartY

                if (resizingCorner != null) {
                    val newR = RectF(activeOriginal)
                    when (resizingCorner) {
                        Corner.TL -> { newR.left += dx; newR.top += dy }
                        Corner.TR -> { newR.right += dx; newR.top += dy }
                        Corner.BL -> { newR.left += dx; newR.bottom += dy }
                        Corner.BR -> { newR.right += dx; newR.bottom += dy }
                        else -> {}
                    }
                    normalizeRectInBounds(newR)
                    r.set(newR)
                } else {
                    val newR = RectF(activeOriginal)
                    newR.offset(dx, dy)
                    keepRectInside(newR)
                    r.set(newR)
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeIndex = -1
                resizingCorner = null
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun snapshot() {
        undoStack.addLast(rects.map { RectF(it) })
        if (undoStack.size > 30) undoStack.removeFirst()
    }

    private fun hitRectIndex(x: Float, y: Float): Int {
        for (i in rects.indices.reversed()) {
            if (rects[i].contains(x, y)) return i
        }
        return -1
    }

    private fun cornerAt(r: RectF, x: Float, y: Float): Corner? {
        val tol = 32f
        fun near(ax: Float, ay: Float) = abs(x - ax) < tol && abs(y - ay) < tol
        return when {
            near(r.left, r.top) -> Corner.TL
            near(r.right, r.top) -> Corner.TR
            near(r.left, r.bottom) -> Corner.BL
            near(r.right, r.bottom) -> Corner.BR
            else -> null
        }
    }

    private fun normalizeRectInBounds(r: RectF) {
        // Ensure left<right, top<bottom and within bounds
        val minSize = 24f
        val l = min(r.left, r.right)
        val rr = max(r.left, r.right)
        val t = min(r.top, r.bottom)
        val bb = max(r.top, r.bottom)

        r.left = l
        r.right = max(l + minSize, rr)
        r.top = t
        r.bottom = max(t + minSize, bb)

        keepRectInside(r)
    }

    private fun keepRectInside(r: RectF) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        if (r.left < 0f) r.offset(-r.left, 0f)
        if (r.top < 0f) r.offset(0f, -r.top)
        if (r.right > w) r.offset(w - r.right, 0f)
        if (r.bottom > h) r.offset(0f, h - r.bottom)
    }
}
