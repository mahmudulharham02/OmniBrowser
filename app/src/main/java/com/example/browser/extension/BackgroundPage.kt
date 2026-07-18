package com.example.browser.extension

import android.content.Context
import android.webkit.WebView
import com.example.data.ExtensionManifest
import com.example.data.InstalledExtensionEntity
import com.example.data.repository.ExtensionRepository
import java.io.File

class BackgroundPage(
    private val context: Context,
    private val extension: InstalledExtensionEntity,
    private val manifest: ExtensionManifest,
    private val extensionRepository: ExtensionRepository,
    private val messageBus: MessageBus
) {
    private var webView: WebView? = null

    fun start() {
        val wv = WebView(context).apply {
            settings.javaScriptEnabled = true
            addJavascriptInterface(
                BridgeInterface(
                    extensionId = extension.id,
                    extensionRepository = extensionRepository,
                    messageBus = messageBus,
                    getWebView = { this }
                ),
                "OmniExtensionBridge"
            )
        }
        webView = wv

        val backgroundHtml = buildBackgroundHtml()
        
        wv.loadDataWithBaseURL(
            "https://omnibrowser.local/extension/${extension.id}/",
            backgroundHtml,
            "text/html",
            "UTF-8",
            null
        )
    }

    fun stop() {
        webView?.post {
            webView?.destroy()
            webView = null
        }
    }

    private fun buildBackgroundHtml(): String {
        val scripts = manifest.background?.scripts ?: emptyList()
        val scriptElements = StringBuilder()
        
        val bootstrap = """
            <script>
            window._chromeListeners = [];
            window._chromeCallbacks = {};
            window.chrome = {
                runtime: {
                    sendMessage: function(msg, target, cb) {
                        var cbId = Math.random().toString(36).substring(2);
                        if (typeof target === 'function') { cb = target; target = null; }
                        if (cb) window._chromeCallbacks[cbId] = cb;
                        OmniExtensionBridge.sendMessage(JSON.stringify(msg), target || null, cbId);
                    },
                    onMessage: {
                        addListener: function(listener) {
                            window._chromeListeners.push(listener);
                        }
                    }
                },
                storage: {
                    local: {
                        get: function(keys, cb) {
                            var cbId = Math.random().toString(36).substring(2);
                            window._chromeCallbacks[cbId] = cb;
                            OmniExtensionBridge.storageGet(JSON.stringify(keys), cbId);
                        },
                        set: function(items, cb) {
                            var cbId = Math.random().toString(36).substring(2);
                            if (cb) window._chromeCallbacks[cbId] = cb;
                            OmniExtensionBridge.storageSet(JSON.stringify(items), cbId);
                        }
                    }
                }
            };
            window.browser = window.chrome;
            
            function deliverMessage(fromId, payload) {
                var msg = JSON.parse(payload);
                window._chromeListeners.forEach(function(listener) {
                    try {
                        listener(msg, { id: fromId }, function() {});
                    } catch(e) { console.error(e); }
                });
            }
            </script>
        """.trimIndent()

        scriptElements.append(bootstrap)

        for (scriptFile in scripts) {
            val file = File(extension.sourcePath, scriptFile)
            if (file.exists()) {
                val code = file.readText()
                scriptElements.append("<script>$code</script>\n")
            }
        }
        
        return """
            <!DOCTYPE html>
            <html>
            <head><title>${manifest.name} Background</title></head>
            <body>
            $scriptElements
            </body>
            </html>
        """.trimIndent()
    }
    
    fun deliverMessage(fromId: String, payload: String) {
        webView?.post {
            val escapedPayload = payload.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r")
            webView?.evaluateJavascript("deliverMessage('$fromId', '$escapedPayload')", null)
        }
    }
}
