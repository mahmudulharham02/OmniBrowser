package com.example.browser.extension

import android.webkit.WebView
import java.io.File
import kotlinx.coroutines.flow.first

class ContentScriptInjector(private val extensionHost: ExtensionHost) {

    fun injectForPage(webView: WebView, url: String, runAtTiming: String) {
        val activeExtensions = extensionHost.getLoadedExtensions()
        for ((entity, manifest) in activeExtensions) {
            if (!entity.enabled) continue

            for (script in manifest.contentScripts) {
                if (!matchesUrlPattern(url, script.matches)) continue
                if (script.runAt != runAtTiming) continue

                // 1. Inject CSS
                for (cssFile in script.css) {
                    val cssCode = extensionHost.readExtensionFile(entity, cssFile) ?: continue
                    val escapedCss = cssCode.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                    
                    val injectCssJs = """
                        (function() {
                            var style = document.createElement('style');
                            style.type = 'text/css';
                            style.textContent = "$escapedCss";
                            (document.head || document.documentElement).appendChild(style);
                        })();
                    """.trimIndent()
                    webView.evaluateJavascript(injectCssJs, null)
                }

                // 2. Inject JS (Pre-bootstrapped with chrome API)
                for (jsFile in script.js) {
                    val jsCode = extensionHost.readExtensionFile(entity, jsFile) ?: continue
                    
                    // Expose chrome.* APIs to the content script
                    val bootstrapJs = """
                        (function() {
                            if (!window.chrome) {
                                window._chromeListeners = window._chromeListeners || [];
                                window._chromeCallbacks = window._chromeCallbacks || {};
                                window.chrome = {
                                    runtime: {
                                        sendMessage: function(msg, cb) {
                                            var cbId = Math.random().toString(36).substring(2);
                                            if (cb) window._chromeCallbacks[cbId] = cb;
                                            OmniExtensionBridge.sendMessage(JSON.stringify(msg), null, cbId);
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
                                
                                window.deliverMessage = function(fromId, payload) {
                                    var msg = JSON.parse(payload);
                                    window._chromeListeners.forEach(function(listener) {
                                        try {
                                            listener(msg, { id: fromId }, function() {});
                                        } catch(e) { console.error(e); }
                                    });
                                };
                            }
                            
                            // Execute actual script code
                            try {
                                $jsCode
                            } catch(e) {
                                console.error("Error executing content script " + "$jsFile" + ": ", e);
                            }
                        })();
                    """.trimIndent()
                    webView.evaluateJavascript(bootstrapJs, null)
                }
            }
        }
    }

    private fun matchesUrlPattern(url: String, patternList: List<String>): Boolean {
        if (patternList.contains("<all_urls>")) return true
        
        for (pattern in patternList) {
            if (pattern == "<all_urls>") return true
            
            try {
                // Convert simple chrome match patterns to regex
                // e.g. *://*.google.com/* -> .*:.*\.google\.com.*
                var regexStr = pattern
                    .replace(".", "\\.")
                    .replace("*", ".*")
                    .replace("?", "\\?")
                
                if (url.matches(Regex(regexStr))) {
                    return true
                }
            } catch (e: Exception) {
                // Ignore pattern syntax errors
            }
        }
        return false
    }
}
