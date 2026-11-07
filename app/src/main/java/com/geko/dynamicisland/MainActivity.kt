package com.geko.dynamicisland

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : Activity() {
    private lateinit var overlayStatus: TextView
    private lateinit var notificationAccessStatus: TextView
    private lateinit var serviceStatus: TextView
    private lateinit var grantOverlayButton: Button
    private lateinit var grantNotificationAccessButton: Button
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }
    override fun onResume() {
        super.onResume()
        refresh()
    }
    private fun hasNotificationAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)

    private fun refresh() {
        val canDraw = Settings.canDrawOverlays(this)
        val notificationAccess = hasNotificationAccess()
        val running = IslandService.isRunning
        overlayStatus.setText(if (canDraw) R.string.status_overlay_ok else R.string.status_overlay_missing)
        overlayStatus.setTextColor(if (canDraw) COLOR_OK else COLOR_WARN)
        notificationAccessStatus.setText(if (notificationAccess) R.string.status_access_ok else R.string.status_access_missing)
        notificationAccessStatus.setTextColor(if (notificationAccess) COLOR_OK else COLOR_WARN)
        serviceStatus.setText(if (running) R.string.status_running else R.string.status_stopped)
        serviceStatus.setTextColor(if (running) COLOR_OK else COLOR_MUTED)
        grantOverlayButton.visibility = if (canDraw) View.GONE else View.VISIBLE
        grantNotificationAccessButton.visibility = if (notificationAccess) View.GONE else View.VISIBLE
        startButton.isEnabled = canDraw && !running
        stopButton.isEnabled = running
    }

    private fun onGrantOverlayClicked() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }
    private fun onGrantNotificationAccessClicked() {
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }
    private fun onStartClicked() {
        if (!Settings.canDrawOverlays(this)) {
            onGrantOverlayClicked()
            return
        }
        IslandService.start(this)
        window.decorView.postDelayed({ refresh() }, 400L)
    }
    private fun onStopClicked() {
        IslandService.stop(this)
        window.decorView.postDelayed({ refresh() }, 400L)
    }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun buildUi(): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val root = ScrollView(this).apply {
            setBackgroundColor(0xFF0B0B0F.toInt())
            isFillViewport = true
            addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(dp(24), bars.top + dp(32), dp(24), bars.bottom + dp(24))
            insets
        }
        content.addView(textView(R.string.main_title, 28f, 0xFFFFFFFF.toInt()))
        content.addView(textView(R.string.main_subtitle, 15f, COLOR_MUTED).apply {
            setPadding(0, dp(4), 0, dp(28))
        })
        overlayStatus = textView(R.string.status_overlay_missing, 16f, COLOR_WARN)
        notificationAccessStatus = textView(R.string.status_access_missing, 16f, COLOR_WARN).apply {
            setPadding(0, dp(6), 0, 0)
        }
        serviceStatus = textView(R.string.status_stopped, 16f, COLOR_MUTED).apply {
            setPadding(0, dp(6), 0, dp(24))
        }
        content.addView(overlayStatus)
        content.addView(notificationAccessStatus)
        content.addView(serviceStatus)
        grantOverlayButton = button(R.string.btn_grant_overlay) { onGrantOverlayClicked() }
        grantNotificationAccessButton = button(R.string.btn_grant_access) { onGrantNotificationAccessClicked() }
        startButton = button(R.string.btn_start) { onStartClicked() }
        stopButton = button(R.string.btn_stop) { onStopClicked() }
        content.addView(grantOverlayButton)
        content.addView(grantNotificationAccessButton)
        content.addView(startButton)
        content.addView(stopButton)
        content.addView(textView(R.string.main_hint, 13f, COLOR_MUTED).apply {
            setPadding(0, dp(24), 0, 0)
        })
        content.addView(textView(R.string.main_restricted_hint, 13f, COLOR_MUTED).apply {
            setPadding(0, dp(12), 0, 0)
        })
        return root
    }
    private fun textView(textRes: Int, sizeSp: Float, color: Int): TextView =
        TextView(this).apply {
            setText(textRes)
            textSize = sizeSp
            setTextColor(color)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    private fun button(textRes: Int, onClick: () -> Unit): Button =
        Button(this).apply {
            setText(textRes)
            isAllCaps = false
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
            }
        }
    private companion object {
        const val REQUEST_NOTIFICATIONS = 100
        const val COLOR_OK = 0xFF34C759.toInt()
        const val COLOR_WARN = 0xFFFF9F0A.toInt()
        const val COLOR_MUTED = 0xFF8E8E93.toInt()
    }
}
