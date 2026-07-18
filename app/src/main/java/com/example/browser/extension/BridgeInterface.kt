package com.example.browser.extension

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.example.data.repository.ExtensionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class BridgeInterface(
    private val extensionId: String,
    private val extensionRepository: ExtensionRepository,
    private val messageBus: MessageBus,
    private val getWebView: () -> WebView?
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    @JavascriptInterface
    fun sendMessage(messageJson: String, target: String?, callbackId: String?) {
        messageBus.dispatch(
            from = extensionId,
            to = target,
            payload = messageJson
        )
        if (callbackId != null) {
            coroutineScope.launch {
                getWebView()?.evaluateJavascript(
                    "if (window._chromeCallbacks && window._chromeCallbacks['$callbackId']) { window._chromeCallbacks['$callbackId']({success: true}); delete window._chromeCallbacks['$callbackId']; }",
                    null
                )
            }
        }
    }

    @JavascriptInterface
    fun storageGet(keysJson: String, callbackId: String) {
        coroutineScope.launch {
            val resultObj = JSONObject()
            try {
                val dbValues = withContext(Dispatchers.IO) {
                    extensionRepository.getStorageValues(extensionId)
                }
                
                dbValues.forEach { (k, v) ->
                    try {
                        if (v.startsWith("{") && v.endsWith("}")) {
                            resultObj.put(k, JSONObject(v))
                        } else if (v.startsWith("[") && v.endsWith("]")) {
                            resultObj.put(k, org.json.JSONArray(v))
                        } else {
                            resultObj.put(k, v)
                        }
                    } catch (e: Exception) {
                        resultObj.put(k, v)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            getWebView()?.evaluateJavascript(
                "if (window._chromeCallbacks && window._chromeCallbacks['$callbackId']) { window._chromeCallbacks['$callbackId']($resultObj); delete window._chromeCallbacks['$callbackId']; }",
                null
            )
        }
    }

    @JavascriptInterface
    fun storageSet(itemsJson: String, callbackId: String?) {
        coroutineScope.launch {
            try {
                val items = JSONObject(itemsJson)
                val keys = items.keys()
                withContext(Dispatchers.IO) {
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = items.get(key).toString()
                        extensionRepository.insertStorageValue(extensionId, key, value)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (callbackId != null) {
                getWebView()?.evaluateJavascript(
                    "if (window._chromeCallbacks && window._chromeCallbacks['$callbackId']) { window._chromeCallbacks['$callbackId'](); delete window._chromeCallbacks['$callbackId']; }",
                    null
                )
            }
        }
    }
}
