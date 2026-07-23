package com.example.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class SearchSuggestion(
    val text: String,
    val isUrl: Boolean = false,
    val subtitle: String? = null
)

object SearchSuggestionRepository {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .build()
    }

    suspend fun fetchSuggestions(query: String): List<SearchSuggestion> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        val suggestions = mutableListOf<SearchSuggestion>()

        // Direct domain suggestion if input looks like a domain without scheme
        if (trimmed.contains(".") && !trimmed.contains(" ") && !trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            suggestions.add(
                SearchSuggestion(
                    text = "https://$trimmed",
                    isUrl = true,
                    subtitle = "Go to website"
                )
            )
        }

        try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://suggestqueries.google.com/complete/search?client=firefox&q=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (!body.isNullOrBlank()) {
                val jsonArray = JSONArray(body)
                if (jsonArray.length() >= 2) {
                    val predictions = jsonArray.getJSONArray(1)
                    for (i in 0 until minOf(predictions.length(), 6)) {
                        val itemText = predictions.getString(i)
                        if (suggestions.none { it.text.equals(itemText, ignoreCase = true) }) {
                            val isUrl = SearchEngine.isValidUrl(itemText) || (itemText.contains(".") && !itemText.contains(" "))
                            suggestions.add(
                                SearchSuggestion(
                                    text = itemText,
                                    isUrl = isUrl,
                                    subtitle = if (isUrl) "Website" else "Search suggestion"
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Silently handle offline or parsing errors
        }

        suggestions
    }
}
