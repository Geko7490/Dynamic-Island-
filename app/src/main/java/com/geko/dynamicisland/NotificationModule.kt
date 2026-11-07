package com.geko.dynamicisland

import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class NotificationModule(private val context: Context, private val onOpen: () -> Unit) : IslandModule {
    override val id = "notifications"
    override val priority = 40
    override val isAvailable: Boolean get() = NotificationRepository.count > 0
    override val collapsedView = FrameLayout(context)
    override val expandedView = LinearLayout(context)
    private val iconView = ImageView(context)
    private val countView = TextView(context)
    init {
        collapsedView.addView(iconView, FrameLayout.LayoutParams(context.dp(20), context.dp(20), Gravity.START or Gravity.CENTER_VERTICAL).apply {
            marginStart = context.dp(12)
        })
        countView.apply {
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        collapsedView.addView(countView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END or Gravity.CENTER_VERTICAL
        ).apply { marginEnd = context.dp(14) })
        expandedView.orientation = LinearLayout.VERTICAL
        expandedView.gravity = Gravity.CENTER_VERTICAL
    }
    fun update() {
        val items = NotificationRepository.recent
        val latest = items.firstOrNull()
        iconView.setImageDrawable(latest?.icon?.let { copyOf(it) })
        countView.text = if (items.size > 1) items.size.toString() else ""
        expandedView.removeAllViews()
        items.take(MAX_ROWS).forEach { notification ->
            expandedView.addView(row(notification), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dp(ROW_HEIGHT_DP)))
        }
    }
    private fun row(notification: IslandNotification): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dp(16), 0, context.dp(16), 0)
            isClickable = true
            setOnClickListener { open(notification) }
        }
        val icon = ImageView(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF3A3A3C.toInt())
            }
            setImageDrawable(notification.icon?.let { copyOf(it) })
        }
        row.addView(icon, LinearLayout.LayoutParams(context.dp(28), context.dp(28)).apply { marginEnd = context.dp(12) })
        val title = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            text = if (notification.title.isBlank()) notification.appName else notification.title
        }
        val body = TextView(context).apply {
            setTextColor(0xFF8E8E93.toInt())
            textSize = 12f
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            text = notification.text
            visibility = if (notification.text.isBlank()) View.GONE else View.VISIBLE
        }
        val texts = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(body)
        }
        row.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        return row
    }
    private fun open(notification: IslandNotification) {
        try { notification.contentIntent?.send() } catch (e: PendingIntent.CanceledException) {}
        onOpen()
    }
    private fun copyOf(drawable: Drawable): Drawable =
        drawable.constantState?.newDrawable(context.resources)?.mutate() ?: drawable
    private companion object {
        const val MAX_ROWS = 3
        const val ROW_HEIGHT_DP = 38
    }
}
