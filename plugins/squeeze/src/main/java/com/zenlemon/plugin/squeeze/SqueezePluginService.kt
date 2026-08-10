package com.zenlemon.plugin.squeeze

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner
import kotlin.concurrent.thread

class SqueezePluginService : Service() {

    internal inner class IncomingHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val requestData = msg.data
            val apiVersion = requestData.getInt("api_version", 1)
            val requestId = requestData.getString("request_id") ?: return

            val responseData = Bundle().apply {
                putInt("api_version", apiVersion)
                putString("request_id", requestId)
            }

            try {
                if (msg.what == 5) {
                    val inputUrl = requestData.getString("input_url") ?: ""

                    if (inputUrl.contains("youtube.com") || inputUrl.contains("youtu.be")) {
                        thread {
                            try {
                                val directStreamUrl = extractYoutubeStream(inputUrl)
                                responseData.putBoolean("success", true)
                                responseData.putBoolean("handled", true)
                                responseData.putString("output_url", directStreamUrl)
                            } catch (e: Exception) {
                                responseData.putBoolean("success", false)
                                responseData.putString("message", e.message)
                            }

                            val replyMsg = Message.obtain(null, msg.what).apply { data = responseData }
                            try {
                                msg.replyTo?.send(replyMsg)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        return
                    } else {
                        responseData.putBoolean("success", true)
                        responseData.putBoolean("handled", false)
                    }
                } else if (msg.what == 1) {
                    responseData.putBoolean("success", true)
                    responseData.putString("manifest_json", """{"name": "ZenLemon Squeeze", "capabilities": ["playback.prepare"]}""")
                } else {
                    responseData.putBoolean("success", true)
                    responseData.putBoolean("handled", false)
                }
            } catch (e: Exception) {
                responseData.putBoolean("success", false)
                responseData.putString("message", e.message)
            }

            val replyMsg = Message.obtain(null, msg.what).apply { data = responseData }
            try {
                msg.replyTo?.send(replyMsg)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val messenger = Messenger(IncomingHandler())

    override fun onBind(intent: Intent): IBinder? {
        return if (intent.action == "com.zenlemon.plugin.API") {
            messenger.binder
        } else {
            null
        }
    }

    private fun extractYoutubeStream(youtubeUrl: String): String {
        // REPLACE YOUR_SERVER_IP BELOW WITH YOUR ACTUAL SERVER IP
        val serverApiUrl = "http://100.109.161.124:3000/api/extract?url=" + java.net.URLEncoder.encode(youtubeUrl, "UTF-8")
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
            throw Exception("Server returned code: ${connection.responseCode}")
        }
    }
}
