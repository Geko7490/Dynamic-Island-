package com.geko.dynamicisland

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adayı ekranın üstüne (overlay) ekleyen ve canlı tutan ön plan servisi.
 */
class IslandService : Service() {

    private lateinit var windowManager: WindowManager
    private var island: IslandView? = null

    private val handler = Handler(Looper.getMainLooper())
    private val clockFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private var clockView: TextView? = null
    private val clockTick = object : Runnable {
        override fun run() {
            clockView?.text = clockFormat.format(Date())
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Ön plan servisi 5 saniye içinde startForeground çağırmak zorunda; önce bunu yapıyoruz.
        createNotificationChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        addIsland()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(clockTick)
        island?.let { view ->
            if (view.isAttachedToWindow) windowManager.removeView(view)
        }
        island = null
        super.onDestroy()
    }

    // ---------------------------------------------------------------- ada

    private fun addIsland() {
        val view = IslandView(this)
        buildPlaceholderContent(view)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (TOP_MARGIN_DP * resources.displayMetrics.density).toInt()
            title = "DynamicIslandOverlay"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        view.onStateChanged = { state ->
            handler.removeCallbacks(clockTick)
            if (state == IslandView.State.EXPANDED) handler.post(clockTick)
        }
        view.onLongPress = { openApp() }

        windowManager.addView(view, params)
        island = view
    }

    /**
     * Aşama 1 için örnek içerik: küçük hâlde yeşil bir nokta, büyük hâlde saat.
     * Sonraki aşamalarda bu alanları gerçek modüller dolduracak.
     */
    private fun buildPlaceholderContent(view: IslandView) {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val dot = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF34C759.toInt())
            }
        }
        view.collapsedContent.addView(
            dot,
            FrameLayout.LayoutParams(dp(7), dp(7), Gravity.CENTER_VERTICAL or Gravity.END).apply {
                marginEnd = dp(16)
            }
        )

        val clock = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 34f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            gravity = Gravity.CENTER
            text = clockFormat.format(Date())
        }
        val subtitle = TextView(this).apply {
            setTextColor(0xFF8E8E93.toInt())
            textSize = 13f
            gravity = Gravity.CENTER
            setText(R.string.island_subtitle)
        }
        clockView = clock

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(clock)
            addView(subtitle)
        }
        view.expandedContent.addView(
            column,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    // ---------------------------------------------------------------- bildirim

    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply { setShowBadge(false) }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, IslandService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_island)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.notif_action_stop), stopIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.geko.dynamicisland.action.STOP"
        private const val CHANNEL_ID = "island_service"
        private const val NOTIFICATION_ID = 1

        /** Adanın üstten boşluğu (dp). Sonraki aşamada ayarlardan değiştirilebilir olacak. */
        private const val TOP_MARGIN_DP = 10

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, IslandService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, IslandService::class.java))
        }
    }
}
