package com.example.browser.tab

import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.WebView

object TabThumbnailer {
    fun capture(webView: WebView): Bitmap? {
        return try {
            val width = webView.width.takeIf { it > 0 } ?: 500
            val height = webView.height.takeIf { it > 0 } ?: 800
            val bitmap = Bitmap.createBitmap(width / 2, height / 2, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.scale(0.5f, 0.5f)
            webView.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
