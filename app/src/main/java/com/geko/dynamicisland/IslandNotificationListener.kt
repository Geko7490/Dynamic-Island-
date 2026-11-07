package com.geko.dynamicisland

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class IslandNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationRepository.setConnected(true)
        try { activeNotifications?.forEach { handle(it, alert = false) } } catch (e: Exception) {}
    }
    override fun onListenerDisconnected() {
        NotificationRepository.setConnected(false)
        NotificationRepository.clear()
        try {
            requestRebind(ComponentName(this, IslandNotificationListener::class.java))
        } catch (e: Exception) {}
        super.onListenerDisconnected()
    }
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn != null) handle(sbn, alert = true)
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn != null) NotificationRepository.remove(sbn.key)
    }
    private fun handle(sbn: StatusBarNotification, alert: Boolean) {
        val notification = sbn.notification ?: return
        if (sbn.packageName == packageName) return
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        if ((notification.flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE)) != 0) return
        val category = notification.category
        if (category == Notification.CATEGORY_TRANSPORT || category == Notification.CATEGORY_SERVICE || category == Notification.CATEGORY_PROGRESS) return
        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE) ?: ""
        val body = extras?.getCharSequence(Notification.EXTRA_TEXT) ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: ""
        if (title.isBlank() && body.isBlank()) return
        NotificationRepository.post(
            IslandNotification(
                key = sbn.key,
                packageName = sbn.packageName,
                appName = appLabel(sbn.packageName),
                title = title,
                text = body,
                icon = appIcon(sbn, notification),
                contentIntent = notification.contentIntent,
                postTime = sbn.postTime
            ),
            alert = alert && isAlertWorthy(sbn)
        )
    }
    private fun isAlertWorthy(sbn: StatusBarNotification): Boolean = try {
        val ranking = Ranking()
        if (currentRanking.getRanking(sbn.key, ranking)) ranking.importance >= NotificationManager.IMPORTANCE_DEFAULT else true
    } catch (e: Exception) { true }

    private fun appLabel(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) { pkg }

    private fun appIcon(sbn: StatusBarNotification, notification: Notification): Drawable? = try {
        packageManager.getApplicationIcon(sbn.packageName)
    } catch (e: Exception) {
        try { notification.smallIcon?.loadDrawable(this) } catch (e2: Exception) { null }
    }
}
