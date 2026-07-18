package com.example.browser.extension

import com.example.data.ContentScriptSpec
import com.example.data.BackgroundSpec
import com.example.data.ExtensionManifest
import org.json.JSONObject
import java.io.File

object ExtensionManifestParser {
    fun parse(manifestFile: File): ExtensionManifest {
        val jsonStr = manifestFile.readText()
        return parse(jsonStr)
    }

    fun parse(jsonStr: String): ExtensionManifest {
        val obj = JSONObject(jsonStr)
        val manifestVersion = obj.optInt("manifest_version", 3)
        val name = obj.getString("name")
        val version = obj.getString("version")
        val description = obj.optString("description", null)

        val permissions = mutableListOf<String>()
        val permissionsArr = obj.optJSONArray("permissions")
        if (permissionsArr != null) {
            for (i in 0 until permissionsArr.length()) {
                permissions.add(permissionsArr.getString(i))
            }
        }

        val hostPermissions = mutableListOf<String>()
        val hostPermissionsArr = obj.optJSONArray("host_permissions")
        if (hostPermissionsArr != null) {
            for (i in 0 until hostPermissionsArr.length()) {
                hostPermissions.add(hostPermissionsArr.getString(i))
            }
        }

        val contentScripts = mutableListOf<ContentScriptSpec>()
        val contentScriptsArr = obj.optJSONArray("content_scripts")
        if (contentScriptsArr != null) {
            for (i in 0 until contentScriptsArr.length()) {
                val scriptObj = contentScriptsArr.getJSONObject(i)
                val matches = mutableListOf<String>()
                val matchesArr = scriptObj.getJSONArray("matches")
                for (j in 0 until matchesArr.length()) {
                    matches.add(matchesArr.getString(j))
                }

                val js = mutableListOf<String>()
                val jsArr = scriptObj.optJSONArray("js")
                if (jsArr != null) {
                    for (j in 0 until jsArr.length()) {
                        js.add(jsArr.getString(j))
                    }
                }

                val css = mutableListOf<String>()
                val cssArr = scriptObj.optJSONArray("css")
                if (cssArr != null) {
                    for (j in 0 until cssArr.length()) {
                        css.add(cssArr.getString(j))
                    }
                }

                val runAt = scriptObj.optString("run_at", "document_idle")
                contentScripts.add(ContentScriptSpec(matches, js, css, runAt))
            }
        }

        var backgroundSpec: BackgroundSpec? = null
        val backgroundObj = obj.optJSONObject("background")
        if (backgroundObj != null) {
            val scripts = mutableListOf<String>()
            val scriptsArr = backgroundObj.optJSONArray("scripts")
            if (scriptsArr != null) {
                for (i in 0 until scriptsArr.length()) {
                    scripts.add(scriptsArr.getString(i))
                }
            } else {
                val serviceWorker = backgroundObj.optString("service_worker", "")
                if (serviceWorker.isNotEmpty()) {
                    scripts.add(serviceWorker)
                }
            }
            val persistent = backgroundObj.optBoolean("persistent", false)
            backgroundSpec = BackgroundSpec(scripts, persistent)
        }

        val icons = mutableMapOf<String, String>()
        val iconsObj = obj.optJSONObject("icons")
        if (iconsObj != null) {
            val keys = iconsObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                icons[key] = iconsObj.getString(key)
            }
        }

        return ExtensionManifest(
            manifestVersion = manifestVersion,
            name = name,
            version = version,
            description = description,
            permissions = permissions,
            hostPermissions = hostPermissions,
            contentScripts = contentScripts,
            background = backgroundSpec,
            icons = icons
        )
    }

    fun isSupported(manifest: ExtensionManifest): Boolean {
        val unsupported = listOf("debugger", "desktopCapture")
        return manifest.permissions.none { it in unsupported }
    }
}
