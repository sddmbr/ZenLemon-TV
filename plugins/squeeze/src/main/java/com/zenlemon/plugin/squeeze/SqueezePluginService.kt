package com.zenlemon.plugin.squeeze

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.content.Context
import android.os.Message
import android.os.Messenger
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner
import kotlin.concurrent.thread

class SqueezePluginService : Service() {

    companion object {
        private const val TAG = "SqueezePlugin"
        private const val PREFS_NAME = "squeeze_plugin_prefs"
        private const val PREF_SERVER_URL = "serverUrl"
        private const val DEFAULT_SERVER_URL = "http://192.168.0.35:3000"
    }

    private class IncomingHandler(looper: Looper, private val context: Context) : Handler(looper) {
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
                    responseData.putString("manifest_json", """{"name": "ZenLemon Squeeze", "capabilities": ["playback.prepare", "configuration.schema"], "configurationMode": "host.schema"}""")
                } else if (msg.what == 7) { // MSG_GET_CONFIGURATION_SCHEMA
                    Log.d(TAG, "Configuration schema requested")
                    val schemaJson = """
                        {
                          "schemaVersion": 1,
                          "title": "ZenLemon Squeeze",
                          "description": "Native YouTube extraction settings.",
                          "sections": [
                            {
                              "id": "connection",
                              "title": "Connection",
                              "description": "Extractor endpoint.",
                              "fields": [
                                {
                                  "key": "serverUrl",
                                  "type": "url",
                                  "label": "Extractor Server URL",
                                  "placeholder": "http://192.168.0.35:3000",
                                  "required": true
                                }
                              ]
                            }
                          ]
                        }
                    """.trimIndent()
                    responseData.putBoolean("success", true)
                    responseData.putString("configuration_schema_json", schemaJson)
                } else if (msg.what == 8) { // MSG_GET_CONFIGURATION_VALUES
                    Log.d(TAG, "Configuration values requested")
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val serverUrl = prefs.getString(PREF_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
                    val valuesJson = JSONObject().apply {
                        put("serverUrl", serverUrl)
                    }.toString()
                    responseData.putBoolean("success", true)
                    responseData.putString("configuration_values_json", valuesJson)
                } else if (msg.what == 9) { // MSG_SET_CONFIGURATION_VALUES
                    Log.d(TAG, "Setting configuration values")
                    val valuesJsonStr = requestData.getString("configuration_values_json")
                    if (valuesJsonStr != null) {
                        val valuesJson = JSONObject(valuesJsonStr)
                        val newServerUrl = valuesJson.optString("serverUrl", DEFAULT_SERVER_URL)
                        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit().putString(PREF_SERVER_URL, newServerUrl).apply()
                        responseData.putBoolean("success", true)
                    } else {
                        responseData.putBoolean("success", false)
                        responseData.putString("message", "Missing configuration values")
                    }
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
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            var baseUrl = prefs.getString(PREF_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL

            // Remove trailing slash if present to avoid //api/extract
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.dropLast(1)
            }

            val serverApiUrl = "$baseUrl/api/extract?url=" + java.net.URLEncoder.encode(youtubeUrl, "UTF-8")
            
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

    private var messenger: Messenger? = null

    override fun onCreate() {
        super.onCreate()
        messenger = Messenger(IncomingHandler(Looper.getMainLooper(), this))
    }

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
