package com.zenlemon.plugin.squeeze

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner
import kotlin.concurrent.thread

class SqueezePluginService : Service() {

    companion object {
        private const val TAG = "SqueezePlugin"
    }

    private class IncomingHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            val requestData = msg.data
            val apiVersion = requestData.getInt("api_version", 1)
            val requestId = requestData.getString("request_id") ?: return

            Log.d(TAG, "Received message: ${msg.what} (req: $requestId)")

            val responseData = Bundle().apply {
                putInt("api_version", apiVersion)
                putString("request_id", requestId)
            }

            try {
                if (msg.what == 5) {
                    val inputUrl = requestData.getString("input_url") ?: ""
                    Log.d(TAG, "Preparing playback for URL: $inputUrl")

                    if (inputUrl.contains("youtube.com") || inputUrl.contains("youtu.be")) {
                        val replyTo = msg.replyTo
                        thread {
                            try {
                                val directStreamUrl = extractYoutubeStream(inputUrl)
                                Log.d(TAG, "Successfully extracted stream: $directStreamUrl")
                                responseData.putBoolean("success", true)
                                responseData.putBoolean("handled", true)
                                responseData.putString("output_url", directStreamUrl)
                            } catch (e: Exception) {
                                Log.e(TAG, "Extraction failed", e)
                                responseData.putBoolean("success", false)
                                responseData.putString("message", "Extraction failed: ${e.message}")
                            }

                            val replyMsg = Message.obtain(null, msg.what).apply { data = responseData }
                            try {
                                replyTo?.send(replyMsg)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to send reply", e)
                            }
                        }
                        return
                    } else {
                        Log.d(TAG, "URL not handled by Squeeze: $inputUrl")
                        responseData.putBoolean("success", true)
                        responseData.putBoolean("handled", false)
                    }
                } else if (msg.what == 1) {
                    Log.d(TAG, "Manifest requested")
                    responseData.putBoolean("success", true)
                    responseData.putString("manifest_json", """{"name": "ZenLemon Squeeze", "capabilities": ["playback.prepare"]}""")
                } else {
                    Log.d(TAG, "Unhandled message type: ${msg.what}")
                    responseData.putBoolean("success", true)
                    responseData.putBoolean("handled", false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling message", e)
                responseData.putBoolean("success", false)
                responseData.putString("message", e.message)
            }

            val replyMsg = Message.obtain(null, msg.what).apply { data = responseData }
            try {
                msg.replyTo?.send(replyMsg)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send immediate reply", e)
            }
        }

        private fun extractYoutubeStream(youtubeUrl: String): String {
            // Updated to the current LAN IP for testing. 
            // TODO: In production, this should point to a public domain or be configurable in the UI.
            val serverApiUrl = "http://192.168.0.35:3000/api/extract?url=" + java.net.URLEncoder.encode(youtubeUrl, "UTF-8")
            
            Log.d(TAG, "Connecting to extractor: $serverApiUrl")
            val connection = URL(serverApiUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val scanner = Scanner(connection.inputStream)
                scanner.useDelimiter("\\A")
                val response = if (scanner.hasNext()) scanner.next() else ""
                scanner.close()

                return response.trim()
            } else {
                val errorStream = connection.errorStream?.let { Scanner(it).useDelimiter("\\A").next() }
                throw Exception("Server returned code: ${connection.responseCode}. Details: $errorStream")
            }
        }
    }

    private var messenger: Messenger? = Messenger(IncomingHandler(Looper.getMainLooper()))

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "Service bound: ${intent.action}")
        return if (intent.action == "com.zenlemon.plugin.API") {
            messenger?.binder
        } else {
            null
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroying")
        messenger = null
        super.onDestroy()
    }
}
