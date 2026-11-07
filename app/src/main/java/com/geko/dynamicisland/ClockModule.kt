package com.geko.dynamicisland

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClockModule(context: Context) : IslandModule {
    override val id = "clock"
    override val priority = 10
    override val isAvailable: Boolean get() = true
    override val collapsedView = FrameLayout(context)
    override val expandedView = LinearLayout(context)
    private val timeView = TextView(context)
    private val dateView = TextView(context)
    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("d MMMM EEEE", Locale.getDefault())
    private var expanded = false
    private val tick = object : Runnable {
        override fun run() {
            render()
            if (expanded) handler.postDelayed(this, 1000L)
        }
    }
    init {
        val dot = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF34C759.toInt())
            }
        }
        collapsedView.addView(dot, FrameLayout.LayoutParams(context.dp(7), context.dp(7), Gravity.CENTER_VERTICAL or Gravity.END).apply { marginEnd = context.dp(16) })
        timeView.apply {
            setTextColor(Color.WHITE)
            textSize = 34f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            gravity = Gravity.CENTER
        }
        dateView.apply {
            setTextColor(0xFF8E8E93.toInt())
            textSize = 13f
            gravity = Gravity.CENTER
        }
        expandedView.orientation = LinearLayout.VERTICAL
        expandedView.gravity = Gravity.CENTER
        expandedView.addView(timeView)
        expandedView.addView(dateView)
        render()
    }
    override fun onExpandedChanged(expanded: Boolean) {
        this.expanded = expanded
        handler.removeCallbacks(tick)
        if (expanded) tick.run()
    }
    private fun render() {
        val now = Date()
        timeView.text = timeFormat.format(now)
        dateView.text = dateFormat.format(now)
    }
}
