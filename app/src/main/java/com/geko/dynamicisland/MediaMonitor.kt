package com.geko.dynamicisland

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

data class MediaInfo(
    val packageName: String,
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val isPlaying: Boolean,
    val durationMs: Long,
    private val positionMs: Long,
    private val speed: Float,
    private val updatedAt: Long,
    val controller: MediaController
) {
    fun currentPosition(): Long {
        val upper = if (durationMs > 0) durationMs else Long.MAX_VALUE
        if (!isPlaying) return positionMs.coerceIn(0L, upper)
        val elapsed = SystemClock.elapsedRealtime() - updatedAt
        return (positionMs + (elapsed * speed).toLong()).coerceIn(0L, upper)
    }
}

class MediaMonitor(private val context: Context, private val onChanged: (MediaInfo?) -> Unit) {
    private class Tracked(val controller: MediaController, val callback: MediaController.Callback)
    private val handler = Handler(Looper.getMainLooper())
    private val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val component = ComponentName(context, IslandNotificationListener::class.java)
    private val tracked = LinkedHashMap<MediaSession.Token, Tracked>()
    private var listening = false
    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateSessions(controllers)
    }
    fun start() {
        if (listening) return
        try {
            sessionManager.addOnActiveSessionsChangedListener(sessionsListener, component, handler)
            listening = true
            updateSessions(sessionManager.getActiveSessions(component))
        } catch (e: SecurityException) {
            listening = false
            onChanged(null)
        }
    }
    fun stop() {
        if (listening) {
            try { sessionManager.removeOnActiveSessionsChangedListener(sessionsListener) } catch (e: Exception) {}
            listening = false
        }
        tracked.values.forEach { safeUnregister(it) }
        tracked.clear()
    }
    fun restart() {
        stop()
        start()
    }
    private fun updateSessions(controllers: List<MediaController>?) {
        val current = controllers.orEmpty()
        val tokens = current.map { it.sessionToken }.toSet()
        val iterator = tracked.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key !in tokens) {
                safeUnregister(entry.value)
                iterator.remove()
            }
        }
        for (controller in current) {
            val token = controller.sessionToken
            if (tracked.containsKey(token)) continue
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
                override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
                override fun onSessionDestroyed() = publish()
            }
            controller.registerCallback(callback, handler)
            tracked[token] = Tracked(controller, callback)
        }
        publish()
    }
    private fun safeUnregister(entry: Tracked) {
        try { entry.controller.unregisterCallback(entry.callback) } catch (e: Exception) {}
    }
    private fun publish() { onChanged(select()) }
    private fun select(): MediaInfo? {
        val controllers = tracked.values.map { it.controller }
        val playing = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        val chosen = playing ?: controllers.firstOrNull {
            val state = it.playbackState?.state
            (state == PlaybackState.STATE_PAUSED || state == PlaybackState.STATE_BUFFERING) && it.metadata != null
        } ?: return null
        return build(chosen)
    }
    private fun build(controller: MediaController): MediaInfo {
        val metadata = controller.metadata
        val state = controller.playbackState
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: appLabel(controller.packageName)
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: ""
        val art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        return MediaInfo(
            packageName = controller.packageName,
            title = title,
            artist = artist,
            art = art,
            isPlaying = state?.state == PlaybackState.STATE_PLAYING,
            durationMs = duration,
            positionMs = state?.position ?: 0L,
            speed = state?.playbackSpeed ?: 1f,
            updatedAt = state?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
            controller = controller
        )
    }
    private fun appLabel(pkg: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) { pkg }
}
