package com.geko.dynamicisland

import android.Manifest
import android.app.Activity
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
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Ana ekran: izin durumunu gösterir, adayı başlatır/durdurur.
 * Arayüz kodla kuruluyor (XML yok) ki dosya sayısı az kalsın.
 */
class MainActivity : Activity() {

    private lateinit var permissionStatus: TextView
    private lateinit var serviceStatus: TextView
    private lateinit var grantButton: Button
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

    private fun refresh() {
        val canDraw = Settings.canDrawOverlays(this)
        val running = IslandService.isRunning

        permissionStatus.setText(
            if (canDraw) R.string.status_permission_ok else R.string.status_permission_missing
        )
        permissionStatus.setTextColor(if (canDraw) COLOR_OK else COLOR_WARN)

        serviceStatus.setText(if (running) R.string.status_running else R.string.status_stopped)
        serviceStatus.setTextColor(if (running) COLOR_OK else COLOR_MUTED)

        grantButton.visibility = if (canDraw) View.GONE else View.VISIBLE
        startButton.isEnabled = canDraw && !running
        stopButton.isEnabled = running
    }

    private fun onGrantClicked() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun onStartClicked() {
        if (!Settings.canDrawOverlays(this)) {
            onGrantClicked()
            return
        }
        IslandService.start(this)
        // Servis kendi süreciyle başlıyor; durum bayrağı birazdan güncellenir.
        window.decorView.postDelayed({ refresh() }, 400L)
    }

    private fun onStopClicked() {
        IslandService.stop(this)
        window.decorView.postDelayed({ refresh() }, 400L)
    }

    // ---------------------------------------------------------------- arayüz

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(0xFF0B0B0F.toInt())
        }

        // Android 15'te uygulamalar kenardan kenara çizilir; sistem çubukları kadar boşluk bırak.
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(dp(24), bars.top + dp(32), dp(24), bars.bottom + dp(24))
            insets
        }

        root.addView(textView(R.string.main_title, 28f, 0xFFFFFFFF.toInt()))
        root.addView(textView(R.string.main_subtitle, 15f, COLOR_MUTED).apply {
            setPadding(0, dp(4), 0, dp(28))
        })

        permissionStatus = textView(R.string.status_permission_missing, 16f, COLOR_WARN)
        serviceStatus = textView(R.string.status_stopped, 16f, COLOR_MUTED).apply {
            setPadding(0, dp(6), 0, dp(24))
        }
        root.addView(permissionStatus)
        root.addView(serviceStatus)

        grantButton = button(R.string.btn_grant) { onGrantClicked() }
        startButton = button(R.string.btn_start) { onStartClicked() }
        stopButton = button(R.string.btn_stop) { onStopClicked() }
        root.addView(grantButton)
        root.addView(startButton)
        root.addView(stopButton)

        root.addView(textView(R.string.main_hint, 13f, COLOR_MUTED).apply {
            setPadding(0, dp(24), 0, 0)
        })

        return root
    }

    private fun textView(textRes: Int, sizeSp: Float, color: Int): TextView =
        TextView(this).apply {
            setText(textRes)
            textSize = sizeSp
            setTextColor(color)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

    private fun button(textRes: Int, onClick: () -> Unit): Button =
        Button(this).apply {
            setText(textRes)
            isAllCaps = false
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }

    private companion object {
        const val REQUEST_NOTIFICATIONS = 100
        const val COLOR_OK = 0xFF34C759.toInt()
        const val COLOR_WARN = 0xFFFF9F0A.toInt()
        const val COLOR_MUTED = 0xFF8E8E93.toInt()
    }
}
