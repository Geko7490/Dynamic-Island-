package com.geko.dynamicisland

import android.view.View

interface IslandModule {
    val id: String
    val priority: Int
    val isAvailable: Boolean
    val collapsedView: View
    val expandedView: View
    fun onExpandedChanged(expanded: Boolean) {}
}
