package com.geko.dynamicisland

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.media.session.MediaController
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class MediaModule(private val context: Context) : IslandModule {
    override val id = "media"
    override val priority = 50
    override val isAvailable: Boolean get() = info != null
    private var info: MediaInfo? = null
    private var expanded = false
    private val handler = Handler(Looper.getMainLooper())
    override val collapsedView = FrameLayout(context)
    override val expandedView = LinearLayout(context)
    private val smallArt = roundedImage(11)
    private val equalizer = EqualizerView(context)
    private val bigArt = roundedImage(14)
    private val titleView = TextView(context)
    private val artistView = TextView(context)
    private val progressLine = ProgressLineView(context)
    private val previousButton = controlButton(R.drawable.ic_skip_previous, R.string.a11y_previous) {
        runControl { it.skipToPrevious() }
    }
    private val playButton = controlButton(R.drawable.ic_play, R.string.a11y_play) {
        runControl { if (info?.isPlaying == true) it.pause() else it.play() }
    }
    private val nextButton = controlButton(R.drawable.ic_skip_next, R.string.a11y_next) {
        runControl { it.skipToNext() }
    }
    private val tick = object : Runnable {
        override fun run() {
            updateProgress()
            if (expanded && info?.isPlaying == true) handler.postDelayed(this, 500L)
        }
    }
    init {
        collapsedView.addView(smallArt, FrameLayout.LayoutParams(context.dp(22), context.dp(22), Gravity.START or Gravity.CENTER_VERTICAL).apply {
            marginStart = context.dp(9)
        })
        collapsedView.addView(equalizer, FrameLayout.LayoutParams(context.dp(14), context.dp(14), Gravity.END or Gravity.CENTER_VERTICAL).apply {
            marginEnd = context.dp(14)
        })
        titleView.apply {
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
        }
        artistView.apply {
            setTextColor(0xFF8E8E93.toInt())
            textSize = 13f
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
        }
        val controls = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(previousButton, LinearLayout.LayoutParams(context.dp(44), context.dp(44)))
            addView(playButton, LinearLayout.LayoutParams(context.dp(48), context.dp(48)))
            addView(nextButton, LinearLayout.LayoutParams(context.dp(44), context.dp(44)))
        }
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            addView(titleView)
            addView(artistView)
            addView(progressLine, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dp(3)).apply {
                topMargin = context.dp(8)
            })
            addView(controls, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dp(2)
            })
        }
        expandedView.orientation = LinearLayout.HORIZONTAL
        expandedView.gravity = Gravity.CENTER_VERTICAL
        expandedView.setPadding(context.dp(16), context.dp(12), context.dp(16), context.dp(12))
        expandedView.addView(bigArt, LinearLayout.LayoutParams(context.dp(84), context.dp(84)))
        expandedView.addView(column, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
            marginStart = context.dp(14)
        })
    }
    fun update(newInfo: MediaInfo?) {
        info = newInfo
        val current = newInfo ?: run {
            handler.removeCallbacks(tick)
            return
        }
        titleView.text = current.title
        artistView.text = current.artist
        artistView.visibility = if (current.artist.isBlank()) View.GONE else View.VISIBLE
        bindArt(smallArt, current.art, 4)
        bindArt(bigArt, current.art, 24)
        equalizer.setPlaying(current.isPlaying)
        playButton.setImageResource(if (current.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        playButton.contentDescription = context.getString(if (current.isPlaying) R.string.a11y_pause else R.string.a11y_play)
        handler.removeCallbacks(tick)
        if (expanded) tick.run() else updateProgress()
    }
    override fun onExpandedChanged(expanded: Boolean) {
        this.expanded = expanded
        handler.removeCallbacks(tick)
        if (expanded) tick.run()
    }
    private fun updateProgress() {
        val current = info ?: return
        if (current.durationMs > 0) {
            progressLine.visibility = View.VISIBLE
            progressLine.fraction = current.currentPosition().toFloat() / current.durationMs
        } else {
            progressLine.visibility = View.INVISIBLE
        }
    }
    private fun runControl(action: (MediaController.TransportControls) -> Unit) {
        try { info?.controller?.transportControls?.let(action) } catch (e: Exception) {}
    }
    private fun bindArt(view: ImageView, bitmap: Bitmap?, placeholderPaddingDp: Int) {
        if (bitmap != null) {
            view.setPadding(0, 0, 0, 0)
            view.scaleType = ImageView.ScaleType.CENTER_CROP
            view.setImageBitmap(bitmap)
        } else {
            val padding = context.dp(placeholderPaddingDp)
            view.setPadding(padding, padding, padding, padding)
            view.scaleType = ImageView.ScaleType.FIT_CENTER
            view.setImageResource(R.drawable.ic_music_note)
        }
    }
    private fun roundedImage(radiusDp: Int): ImageView = ImageView(context).apply {
        setBackgroundColor(0xFF2C2C2E.toInt())
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, context.dp(radiusDp).toFloat())
            }
        }
    }
    private fun controlButton(iconRes: Int, descriptionRes: Int, onClick: () -> Unit): ImageView =
        ImageView(context).apply {
            setImageResource(iconRes)
            contentDescription = context.getString(descriptionRes)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            val padding = context.dp(10)
            setPadding(padding, padding, padding, padding)
            isClickable = true
            setOnClickListener { onClick() }
        }
}
