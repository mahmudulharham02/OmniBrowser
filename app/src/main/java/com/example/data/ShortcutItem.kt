package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ShortcutItem(
    val title: String,
    val url: String,
    val isPinned: Boolean = false
)

object ShortcutStorage {
    private const val PREFS_NAME = "shortcuts_pref"
    private const val KEY_SHORTCUTS = "shortcuts_list"

    fun loadShortcuts(context: Context): List<ShortcutItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_SHORTCUTS, null)
        if (jsonString == null) {
            // Return defaults
            return listOf(
                ShortcutItem("Google", "https://www.google.com", isPinned = true),
                ShortcutItem("DuckDuckGo", "https://www.duckduckgo.com", isPinned = true),
                ShortcutItem("GitHub", "https://github.com", isPinned = false),
                ShortcutItem("Wikipedia", "https://www.wikipedia.org", isPinned = false)
            )
        }
        return try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<ShortcutItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ShortcutItem(
                        title = obj.getString("title"),
                        url = obj.getString("url"),
                        isPinned = obj.optBoolean("isPinned", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveShortcuts(context: Context, shortcuts: List<ShortcutItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        shortcuts.forEach { item ->
            val obj = JSONObject()
            obj.put("title", item.title)
            obj.put("url", item.url)
            obj.put("isPinned", item.isPinned)
            array.put(obj)
        }
        prefs.edit().putString(KEY_SHORTCUTS, array.toString()).apply()
    }
}
