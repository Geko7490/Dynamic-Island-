package com.geko.dynamicisland

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

class IslandService : Service() {
    private lateinit var windowManager: WindowManager
    private var island: IslandView? = null
    private var mediaMonitor: MediaMonitor? = null
    private var repositoryListener: NotificationRepository.Listener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(),
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
        repositoryListener?.let { NotificationRepository.removeListener(it) }
        repositoryListener = null
        mediaMonitor?.stop()
        mediaMonitor = null
        island?.let { view ->
            if (view.isAttachedToWindow) windowManager.removeView(view)
        }
        island = null
        super.onDestroy()
    }

    private fun addIsland() {
        val view = IslandView(this)
        val controller = IslandController(view)
        val clockModule = ClockModule(this)
        val mediaModule = MediaModule(this)
        val notificationModule = NotificationModule(this) { view.collapse() }
        controller.register(clockModule)
        controller.register(mediaModule)
        controller.register(notificationModule)

        val listener = object : NotificationRepository.Listener {
            override fun onPosted(notification: IslandNotification, alert: Boolean) {
                notificationModule.update()
                controller.refresh()
                if (alert) controller.alert(notificationModule)
            }
            override fun onRemoved(key: String) {
                notificationModule.update()
                controller.refresh()
            }
            override fun onConnectionChanged(connected: Boolean) {
                if (connected) mediaMonitor?.restart()
            }
        }
        NotificationRepository.addListener(listener)
        repositoryListener = listener
        notificationModule.update()

        val monitor = MediaMonitor(this) { info ->
            mediaModule.update(info)
            controller.refresh()
        }
        mediaMonitor = monitor
        monitor.start()
        controller.refresh()
        view.onLongPress = { openApp() }

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
        windowManager.addView(view, params)
        island = view
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

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
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, IslandService::class.java).setAction(ACTION_STOP),
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
        private const val TOP_MARGIN_DP = 10
        @Volatile var isRunning: Boolean = false
            private set
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, IslandService::class.java))
        }
        fun stop(context: Context) {
            context.stopService(Intent(context, IslandService::class.java))
        }
    }
}
