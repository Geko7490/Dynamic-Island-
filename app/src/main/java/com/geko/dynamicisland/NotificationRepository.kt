package com.geko.dynamicisland

import android.app.PendingIntent
import android.graphics.drawable.Drawable

data class IslandNotification(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: CharSequence,
    val text: CharSequence,
    val icon: Drawable?,
    val contentIntent: PendingIntent?,
    val postTime: Long
)

object NotificationRepository {
    interface Listener {
        fun onPosted(notification: IslandNotification, alert: Boolean)
        fun onRemoved(key: String)
        fun onConnectionChanged(connected: Boolean) {}
    }
    private const val MAX_ITEMS = 20
    private val items = LinkedHashMap<String, IslandNotification>()
    private val listeners = mutableListOf<Listener>()
    var isConnected: Boolean = false
        private set
    val recent: List<IslandNotification>
        get() = items.values.sortedByDescending { it.postTime }
    val count: Int get() = items.size
    fun addListener(listener: Listener) {
        if (listener !in listeners) listeners.add(listener)
    }
    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }
    fun post(notification: IslandNotification, alert: Boolean) {
        items.remove(notification.key)
        items[notification.key] = notification
        while (items.size > MAX_ITEMS) {
            val oldest = items.keys.first()
            items.remove(oldest)
            listeners.toList().forEach { it.onRemoved(oldest) }
        }
        listeners.toList().forEach { it.onPosted(notification, alert) }
    }
    fun remove(key: String) {
        if (items.remove(key) != null) listeners.toList().forEach { it.onRemoved(key) }
    }
    fun clear() {
        val keys = items.keys.toList()
        items.clear()
        keys.forEach { key -> listeners.toList().forEach { it.onRemoved(key) } }
    }
    fun setConnected(connected: Boolean) {
        isConnected = connected
        listeners.toList().forEach { it.onConnectionChanged(connected) }
    }
}
