package com.example.browser.extension

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class ExtensionMessage(
    val fromId: String,
    val toId: String?,
    val payload: String
)

class MessageBus {
    private val _messages = MutableSharedFlow<ExtensionMessage>(extraBufferCapacity = 100)
    val messages: SharedFlow<ExtensionMessage> = _messages

    fun dispatch(from: String, to: String?, payload: String) {
        _messages.tryEmit(ExtensionMessage(from, to, payload))
    }
}
