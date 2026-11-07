package com.geko.dynamicisland

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.sin

internal fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

class EqualizerView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF34C759.toInt() }
    private val rect = RectF()
    private var phase = 0f
    private var playing = false
    private var animator: ValueAnimator? = null

    fun setPlaying(value: Boolean) {
        playing = value
        syncAnimator()
        invalidate()
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        syncAnimator()
    }
    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }
    private fun syncAnimator() {
        if (playing && isAttachedToWindow) {
            if (animator == null) {
                val anim = ValueAnimator.ofFloat(0f, 1f)
                anim.duration = 900L
                anim.repeatCount = ValueAnimator.INFINITE
                anim.interpolator = LinearInterpolator()
                anim.addUpdateListener {
                    phase = it.animatedValue as Float
                    invalidate()
                }
                animator = anim
                anim.start()
            }
        } else {
            animator?.cancel()
            animator = null
        }
    }
    override fun onDraw(canvas: Canvas) {
        val unit = width / 5f
        val h = height.toFloat()
        for (i in 0 until 3) {
            val level = if (playing) 0.35f + 0.65f * (0.5f + 0.5f * sin(2.0 * PI * (phase + i * 0.33)).toFloat()) else 0.3f
            val barHeight = h * level
            val left = i * 2 * unit
            rect.set(left, h - barHeight, left + unit, h)
            canvas.drawRoundRect(rect, unit / 2f, unit / 2f, paint)
        }
    }
}

class ProgressLineView(context: Context) : View(context) {
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val rect = RectF()
    var fraction: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }
    override fun onDraw(canvas: Canvas) {
        val radius = height / 2f
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, radius, radius, track)
        if (fraction > 0f) {
            rect.set(0f, 0f, width * fraction, height.toFloat())
            canvas.drawRoundRect(rect, radius, radius, fill)
        }
    }
}
