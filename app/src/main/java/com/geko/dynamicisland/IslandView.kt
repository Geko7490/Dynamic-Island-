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
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout

/**
 * Adanın kendisi: siyah, yuvarlak köşeli bir kapsül.
 *
 * İki içerik alanı vardır:
 *  - [collapsedContent]: küçük hâlde görünen içerik
 *  - [expandedContent]: büyük hâlde görünen içerik (sabit boyutlu; ada büyürken ortadan açılır)
 *
 * Sonraki aşamalardaki modüller (bildirim, müzik, şarj, zamanlayıcı) bu iki alana view ekler.
 * Boyut animasyonu [progress] (0 = küçük, 1 = büyük) üzerinden yürür; pencere WRAP_CONTENT
 * olduğu için her karede ölçüm değişir ve pencere kendiliğinden boyutlanır.
 */
class IslandView(context: Context) : FrameLayout(context) {

    enum class State { COLLAPSED, EXPANDED }

    /** Küçük hâlin içeriği. */
    val collapsedContent = FrameLayout(context)

    /** Büyük hâlin içeriği. */
    val expandedContent = FrameLayout(context)

    var state: State = State.COLLAPSED
        private set

    /** Durum değişince (animasyon başlarken) çağrılır. */
    var onStateChanged: ((State) -> Unit)? = null

    /** Uzun basılınca çağrılır. */
    var onLongPress: (() -> Unit)? = null

    /** Büyüdükten sonra kendiliğinden küçülme süresi (ms). 0 = kapalı. */
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
    private val handler = Handler(Looper.getMainLooper())
    private val autoCollapse = Runnable { collapse() }

    private val pillBackground = GradientDrawable().apply { setColor(Color.BLACK) }

    init {
        background = pillBackground
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, currentRadius())
            }
        }

        addView(
            collapsedContent,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        addView(
            expandedContent,
            FrameLayout.LayoutParams(
                expandedWidth,
                expandedHeight,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            )
        )

        contentDescription = context.getString(R.string.a11y_island)
        setOnClickListener { toggle() }
        setOnLongClickListener {
            val handlerSet = onLongPress
            handlerSet?.invoke()
            handlerSet != null
        }

        applyProgress()
    }

    fun expand() = animateTo(State.EXPANDED)

    fun collapse() = animateTo(State.COLLAPSED)

    fun toggle() {
        if (state == State.EXPANDED) collapse() else expand()
    }

    private fun animateTo(target: State) {
        if (state == target && animator == null) return

        handler.removeCallbacks(autoCollapse)
        animator?.cancel()

        state = target
        onStateChanged?.invoke(target)

        val expanding = target == State.EXPANDED
        val anim = ValueAnimator.ofFloat(progress, if (expanding) 1f else 0f)
        anim.duration = if (expanding) 420L else 320L
        anim.interpolator = if (expanding) {
            OvershootInterpolator(0.8f)
        } else {
            PathInterpolator(0.4f, 0f, 0.2f, 1f)
        }
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
                if (expanding && autoCollapseMillis > 0) {
                    handler.postDelayed(autoCollapse, autoCollapseMillis)
                }
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
