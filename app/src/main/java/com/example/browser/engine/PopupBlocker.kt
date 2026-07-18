package com.example.browser.engine

import android.os.Handler
import android.os.Looper

class PopupBlocker(private val adBlockEngine: AdBlockEngine) {
    private var lastUserGestureAt: Long = 0L
    private val mainHandler = Handler(Looper.getMainLooper())

    fun onUserGesture() {
        lastUserGestureAt = System.currentTimeMillis()
    }

    fun shouldAllowPopup(isUserGesture: Boolean): Boolean {
        if (isUserGesture) return true
        val diff = System.currentTimeMillis() - lastUserGestureAt
        return diff < 1000L
    }

    fun onPopupBlocked() {
        adBlockEngine.recordBlock(BlockCategory.POPUP)
    }
}
