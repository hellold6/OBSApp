package com.example.obsapp.network

import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONException
import org.json.JSONObject

class ObsWebSocketListener(private val messageProcessor: (JSONObject) -> Unit) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("ObsWebSocketListener", "Connection opened")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d("ObsWebSocketListener", "Receiving: $text")
        try {
            val message = JSONObject(text)
            messageProcessor(message)
        } catch (e: JSONException) {
            Log.e("ObsWebSocketListener", "Could not parse JSON: $text")
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        Log.d("ObsWebSocketListener", "Receiving bytes: " + bytes.hex())
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d("ObsWebSocketListener", "Closing: $code / $reason")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d("ObsWebSocketListener", "Closed: $code / $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e("ObsWebSocketListener", "Error: " + t.message, t)
    }
}