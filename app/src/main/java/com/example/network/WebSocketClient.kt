package com.example.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

interface NetworkMessageListener {
    fun onConnected()
    fun onDisconnected(reason: String)
    fun onMessageReceived(text: String)
    fun onError(error: Throwable)
}

class WebSocketClient(
    private val listener: NetworkMessageListener
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    var isConnected: Boolean = false
        private set

    fun connect(url: String) {
        disconnect()

        val trimmed = url.trim()
        val cleanUrl = when {
            trimmed.startsWith("https://") -> "wss://" + trimmed.removePrefix("https://")
            trimmed.startsWith("http://") -> "ws://" + trimmed.removePrefix("http://")
            trimmed.startsWith("ws://") || trimmed.startsWith("wss://") -> trimmed
            trimmed.contains(".trycloudflare.com") ||
            trimmed.contains(".pinggy.link") ||
            trimmed.contains(".onrender.com") ||
            trimmed.contains(".railway.app") ||
            trimmed.contains(".loca.lt") -> "wss://$trimmed"
            else -> "ws://$trimmed"
        }

        try {
            val request = Request.Builder()
                .url(cleanUrl)
                .build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    isConnected = true
                    listener.onConnected()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    listener.onMessageReceived(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    isConnected = false
                    webSocket.close(1000, null)
                    listener.onDisconnected(reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    isConnected = false
                    listener.onDisconnected(reason)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    isConnected = false
                    Log.e("WebSocketClient", "Connection failure: ${t.message}")
                    listener.onError(t)
                }
            })
        } catch (e: Exception) {
            isConnected = false
            listener.onError(e)
        }
    }

    fun send(text: String): Boolean {
        return webSocket?.send(text) ?: false
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (ignored: Exception) {
        } finally {
            webSocket = null
            isConnected = false
        }
    }
}
