package com.geko.dynamicisland

import android.os.Handler
import android.os.Looper
import android.view.ViewGroup

class IslandController(private val island: IslandView) {
    private val modules = mutableListOf<IslandModule>()
    private val handler = Handler(Looper.getMainLooper())
    private var shown: IslandModule? = null
    private var pinned: IslandModule? = null

    private val unpin = Runnable {
        if (island.state == IslandView.State.COLLAPSED && pinned != null) {
            pinned = null
            refresh()
        }
    }

    init {
        island.onStateChanged = { state ->
            shown?.onExpandedChanged(state == IslandView.State.EXPANDED)
            handler.removeCallbacks(unpin)
            if (state == IslandView.State.COLLAPSED && pinned != null) {
                handler.postDelayed(unpin, UNPIN_DELAY_MS)
            }
        }
        island.onSwipe = { direction -> cycle(direction) }
    }

    fun register(module: IslandModule) {
        modules.add(module)
        refresh()
    }

    fun refresh() {
        val list = available()
        if (pinned != null && pinned !in list) pinned = null
        show(pinned ?: list.firstOrNull())
    }

    fun alert(module: IslandModule, holdMillis: Long = ALERT_HOLD_MS) {
        if (!module.isAvailable) return
        pinned = module
        handler.removeCallbacks(unpin)
        show(module)
        island.expand(holdMillis)
    }

    private fun available(): List<IslandModule> =
        modules.filter { it.isAvailable }.sortedByDescending { it.priority }

    private fun cycle(direction: Int) {
        val list = available()
        if (list.size < 2) return
        val index = list.indexOf(shown).let { if (it < 0) 0 else it }
        val next = list[(index + direction + list.size) % list.size]
        pinned = next
        show(next)
        island.expand()
    }

    private fun show(module: IslandModule?) {
        if (module === shown) return
        shown?.onExpandedChanged(false)
        island.collapsedContent.removeAllViews()
        island.expandedContent.removeAllViews()
        if (module != null) {
            val matchParent = ViewGroup.LayoutParams.MATCH_PARENT
            island.collapsedContent.addView(module.collapsedView, ViewGroup.LayoutParams(matchParent, matchParent))
            island.expandedContent.addView(module.expandedView, ViewGroup.LayoutParams(matchParent, matchParent))
            module.onExpandedChanged(island.state == IslandView.State.EXPANDED)
        }
        shown = module
    }

    private companion object {
        const val ALERT_HOLD_MS = 5000L
        const val UNPIN_DELAY_MS = 380L
    }
}
