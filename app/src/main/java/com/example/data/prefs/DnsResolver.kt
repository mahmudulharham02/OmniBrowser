package com.example.data.prefs

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetAddress
import kotlinx.coroutines.runBlocking

class AppDns(private val settings: SettingsDataStore) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val dohUrl = runBlocking { settings.customDnsUrl() }
        if (dohUrl.isBlank()) return Dns.SYSTEM.lookup(hostname)
        
        // Prevent resolving the DoH provider itself with DoH
        val dohUri = try { android.net.Uri.parse(dohUrl) } catch (e: Exception) { null }
        val dohHost = dohUri?.host
        if (dohHost != null && hostname.contains(dohHost)) {
            return Dns.SYSTEM.lookup(hostname)
        }

        val resolvedIp = DohResolver.resolve(hostname, dohUrl)
        if (resolvedIp != null) {
            return listOf(InetAddress.getByName(resolvedIp))
        }
        return Dns.SYSTEM.lookup(hostname)
    }
}

object DohResolver {
    private val systemDnsClient = OkHttpClient.Builder()
        .dns(Dns.SYSTEM)
        .build()

    fun resolve(hostname: String, dohUrl: String): String? {
        val url = "$dohUrl?name=$hostname&type=A"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/dns-json")
            .build()
        return runCatching {
            systemDnsClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                val json = JSONObject(body)
                val answers = json.optJSONArray("Answer") ?: return@use null
                for (i in 0 until answers.length()) {
                    val ans = answers.getJSONObject(i)
                    if (ans.optInt("type") == 1) return ans.optString("data")
                }
                null
            }
        }.getOrNull()
    }
}
