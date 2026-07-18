package com.example.browser.engine

import android.net.Uri

object HttpsOnlyEnforcer {
    fun upgradeUrl(url: String): String {
        val uri = try { Uri.parse(url) } catch (e: Exception) { return url }
        if (uri.scheme?.lowercase() == "http") {
            return uri.buildUpon().scheme("https").build().toString()
        }
        return url
    }
}
