package com.geko.dynamicisland

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import kotlin.math.abs

class IslandView(context: Context) : FrameLayout(context) {
    enum class State { COLLAPSED, EXPANDED }
    val collapsedContent = FrameLayout(context)
    val expandedContent = FrameLayout(context)
    var state: State = State.COLLAPSED
        private set
    var onStateChanged: ((State) -> Unit)? = null
    var onLongPress: (() -> Unit)? = null
    var onSwipe: ((Int) -> Unit)? = null
    var autoCollapseMillis: Long = 4000L
    private val density = resources.displayMetrics.density
    private fun dp(value: Int): Int = (value * density).toInt()
    private val collapsedWidth = dp(124)
    private val collapsedHeight = dp(34)
    private val expandedWidth = dp(340)
    private val expandedHeight = dp(132)
    private val collapsedRadius = collapsedHeight / 2f
    private val expandedRadius = dp(30).toFloat()
    private var progress = 0f
    private var animator: ValueAnimator? = null
    private var pendingAutoCollapse = autoCollapseMillis
    private val handler = Handler(Looper.getMainLooper())
    private val autoCollapse = Runnable { collapse() }
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private val pillBackground = GradientDrawable().apply { setColor(Color.BLACK) }

    init {
        background = pillBackground
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, currentRadius())
            }
        }
        addView(collapsedContent, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        addView(expandedContent, FrameLayout.LayoutParams(expandedWidth, expandedHeight, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        contentDescription = context.getString(R.string.a11y_island)
        setOnClickListener { toggle() }
        setOnLongClickListener {
            val callback = onLongPress
            callback?.invoke()
            callback != null
        }
        applyProgress()
    }

    fun expand(autoCollapseAfter: Long = autoCollapseMillis) {
        pendingAutoCollapse = autoCollapseAfter
        if (state == State.EXPANDED) {
            if (animator == null) scheduleAutoCollapse()
            return
        }
        animateTo(State.EXPANDED)
    }
    fun collapse() {
        if (state == State.COLLAPSED) return
        animateTo(State.COLLAPSED)
    }
    fun toggle() {
        if (state == State.EXPANDED) collapse() else expand()
    }
    private fun scheduleAutoCollapse() {
        handler.removeCallbacks(autoCollapse)
        if (pendingAutoCollapse > 0) handler.postDelayed(autoCollapse, pendingAutoCollapse)
    }
    private fun animateTo(target: State) {
        handler.removeCallbacks(autoCollapse)
        animator?.cancel()
        state = target
        onStateChanged?.invoke(target)
        val expanding = target == State.EXPANDED
        val anim = ValueAnimator.ofFloat(progress, if (expanding) 1f else 0f)
        anim.duration = if (expanding) 420L else 320L
        anim.interpolator = if (expanding) OvershootInterpolator(0.8f) else PathInterpolator(0.4f, 0f, 0.2f, 1f)
        anim.addUpdateListener {
            progress = it.animatedValue as Float
            applyProgress()
        }
        anim.addListener(object : AnimatorListenerAdapter() {
            private var cancelled = false
            override fun onAnimationCancel(animation: Animator) {
                cancelled = true
            }
            override fun onAnimationEnd(animation: Animator) {
                if (cancelled) return
                if (animator === anim) animator = null
                if (expanding) scheduleAutoCollapse()
            }
        })
        animator = anim
        anim.start()
    }
    private fun currentRadius(): Float =
        (collapsedRadius + (expandedRadius - collapsedRadius) * progress).coerceAtLeast(0f)
    private fun applyProgress() {
        val p = progress
        collapsedContent.alpha = (1f - p * 2.2f).coerceIn(0f, 1f)
        val expandedAlpha = ((p - 0.45f) / 0.55f).coerceIn(0f, 1f)
        expandedContent.alpha = expandedAlpha
        expandedContent.visibility = if (expandedAlpha > 0.01f) VISIBLE else INVISIBLE
        pillBackground.cornerRadius = currentRadius()
        invalidateOutline()
        requestLayout()
    }
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
            }
            MotionEvent.ACTION_MOVE -> if (state == State.EXPANDED) {
                val dx = ev.rawX - downX
                val dy = ev.rawY - downY
                if (abs(dx) > touchSlop * 2 && abs(dx) > abs(dy) * 1.5f) return true
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                if (state == State.EXPANDED && abs(dx) > dp(48) && abs(dx) > abs(dy) * 1.5f) {
                    val cancel = MotionEvent.obtain(event)
                    cancel.action = MotionEvent.ACTION_CANCEL
                    super.onTouchEvent(cancel)
                    cancel.recycle()
                    onSwipe?.invoke(if (dx < 0) 1 else -1)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = (collapsedWidth + (expandedWidth - collapsedWidth) * progress).toInt().coerceAtLeast(1)
        val h = (collapsedHeight + (expandedHeight - collapsedHeight) * progress).toInt().coerceAtLeast(1)
        collapsedContent.measure(exactly(w), exactly(h))
        expandedContent.measure(exactly(expandedWidth), exactly(expandedHeight))
        setMeasuredDimension(w, h)
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidateOutline()
    }
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
        handler.removeCallbacks(autoCollapse)
    }
    private fun exactly(px: Int): Int = View.MeasureSpec.makeMeasureSpec(px, View.MeasureSpec.EXACTLY)
}
