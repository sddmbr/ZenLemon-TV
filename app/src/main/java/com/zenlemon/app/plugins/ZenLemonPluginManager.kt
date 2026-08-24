package com.zenlemon.app.plugins

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.zenlemon.app.BuildConfig
import com.zenlemon.app.cast.CastMediaRequest
import com.zenlemon.app.tvinput.TvInputChannelSyncManager
import com.zenlemon.domain.model.ActiveLiveSource
import com.zenlemon.domain.model.DrmInfo
import com.zenlemon.domain.model.DrmScheme
import com.zenlemon.domain.model.Provider
import com.zenlemon.domain.model.ProviderEpgSyncMode
import com.zenlemon.domain.model.ProviderType
import com.zenlemon.domain.model.Result
import com.zenlemon.domain.model.StreamInfo
import com.zenlemon.domain.model.StreamType
import com.zenlemon.domain.repository.CombinedM3uRepository
import com.zenlemon.domain.repository.ProviderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@Singleton
class ZenLemonPluginManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val messengerClient: PluginMessengerClient,
    private val providerRepository: ProviderRepository,
    private val combinedM3uRepository: CombinedM3uRepository,
    private val tvInputChannelSyncManager: TvInputChannelSyncManager,
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
    private val prefs = context.getSharedPreferences("zenlemon_plugins", Context.MODE_PRIVATE)

    suspend fun discoverPlugins(): List<InstalledZenLemonPlugin> = withContext(Dispatchers.IO) {
        queryPluginServices()
            .mapNotNull { resolveInfo -> resolvePlugin(resolveInfo) }
            .distinctBy { it.manifest.id }
            .sortedBy { it.displayName.lowercase() }
    }

    suspend fun setPluginEnabled(
        plugin: InstalledZenLemonPlugin,
        enabled: Boolean,
        onProgress: (String) -> Unit = {}
    ): PluginActionResult = withContext(Dispatchers.IO) {
        val command = Bundle().apply {
            putBoolean(ZenLemonPluginContract.KEY_ENABLED, enabled)
        }
        val response = runCatching {
            messengerClient.send(
                packageName = plugin.packageName,
                serviceClassName = plugin.serviceClassName,
                what = ZenLemonPluginContract.MSG_SET_ENABLED,
                data = command,
                timeoutMillis = 120_000L
            )
        }.getOrElse { error ->
            return@withContext PluginActionResult(false, error.message ?: "Plugin did not respond")
        }

        if (!response.getBoolean(ZenLemonPluginContract.KEY_SUCCESS, true)) {
            return@withContext PluginActionResult(
                success = false,
                message = response.getString(ZenLemonPluginContract.KEY_MESSAGE).orEmpty()
                    .ifBlank { "Plugin rejected the request" }
            )
        }

        prefs.edit().putBoolean(enabledKey(plugin.manifest.id), enabled).apply()
        if (enabled && plugin.manifest.hasCapability(ZenLemonPluginContract.CAPABILITY_PROVIDER_M3U)) {
            syncPluginProvider(plugin, onProgress)?.let { return@withContext it }
        } else if (!enabled) {
            removePluginProvider(plugin)?.let { return@withContext it }
        }

        PluginActionResult(
            success = true,
            message = response.getString(ZenLemonPluginContract.KEY_MESSAGE).orEmpty()
                .ifBlank { if (enabled) "Plugin activated" else "Plugin deactivated" }
        )
    }

    suspend fun installApkFromUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        val target = pluginApkFile("local-${System.currentTimeMillis()}.apk")
        runCatching {
            target.parentFile?.mkdirs()
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Cannot open selected APK" }
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }.onFailure { error ->
            return@withContext Result.error("Could not copy selected plugin APK", error)
        }
        launchPackageInstaller(target)
    }

    suspend fun installApkFromUrl(url: String): Result<Unit> = withContext(Dispatchers.IO) {
        val normalizedUrl = url.trim()
        if (!isHttpOrHttpsUrl(normalizedUrl)) {
            return@withContext Result.error("Plugin URL must be http or https")
        }

        val target = pluginApkFile("plugin-${System.currentTimeMillis()}.apk")
        runCatching {
            target.parentFile?.mkdirs()
            val request = Request.Builder().url(normalizedUrl).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                val body = response.body ?: error("Empty response")
                target.outputStream().use { output -> body.byteStream().copyTo(output) }
            }
        }.onFailure { error ->
            return@withContext Result.error("Could not download plugin APK", error)
        }
        launchPackageInstaller(target)
    }

    fun openPluginConfiguration(plugin: InstalledZenLemonPlugin): PluginActionResult {
        val action = plugin.manifest.configurationActivityAction?.takeIf { it.isNotBlank() }
            ?: return PluginActionResult(false, "This plugin has no configuration screen")
        val intent = Intent(action).apply {
            setPackage(plugin.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            PluginActionResult(true, "Opening ${plugin.displayName}")
        }.getOrElse { error ->
            PluginActionResult(false, error.message ?: "Could not open plugin settings")
        }
    }

    suspend fun loadPluginConfiguration(plugin: InstalledZenLemonPlugin): Result<PluginConfigurationSnapshot> =
        withContext(Dispatchers.IO) {
            if (!plugin.manifest.supportsHostRenderedConfiguration) {
                return@withContext Result.error("This plugin does not expose a ZenLemon configuration schema")
            }

            val schemaResponse = runCatching {
                messengerClient.send(
                    packageName = plugin.packageName,
                    serviceClassName = plugin.serviceClassName,
                    what = ZenLemonPluginContract.MSG_GET_CONFIGURATION_SCHEMA,
                    timeoutMillis = 10_000L
                )
            }.getOrElse { error ->
                return@withContext Result.error(error.message ?: "Plugin configuration schema is unavailable")
            }
            if (!schemaResponse.getBoolean(ZenLemonPluginContract.KEY_SUCCESS, false)) {
                return@withContext Result.error(
                    schemaResponse.getString(ZenLemonPluginContract.KEY_MESSAGE).orEmpty()
                        .ifBlank { "Plugin configuration schema is unavailable" }
                )
            }

            val schemaJson = schemaResponse
                .getString(ZenLemonPluginContract.KEY_CONFIGURATION_SCHEMA_JSON)
                .orEmpty()
            val schema = runCatching { json.decodeFromString<PluginConfigurationSchema>(schemaJson) }
                .getOrElse { error ->
                    return@withContext Result.error(error.message ?: "Plugin configuration schema is invalid")
                }
            if (schema.sections.isEmpty() && schema.actions.isEmpty()) {
                return@withContext Result.error("Plugin configuration schema is empty")
            }

            val values = loadPluginConfigurationValues(plugin).getOrNull() ?: JsonObject(emptyMap())
            Result.success(PluginConfigurationSnapshot(plugin, schema, values))
        }

    suspend fun loadPluginConfigurationValues(plugin: InstalledZenLemonPlugin): Result<JsonObject> =
        withContext(Dispatchers.IO) {
            val valuesResponse = runCatching {
                messengerClient.send(
                    packageName = plugin.packageName,
                    serviceClassName = plugin.serviceClassName,
                    what = ZenLemonPluginContract.MSG_GET_CONFIGURATION_VALUES,
                    timeoutMillis = 10_000L
                )
            }.getOrElse { error ->
                return@withContext Result.error(error.message ?: "Plugin configuration values are unavailable")
            }
            if (!valuesResponse.getBoolean(ZenLemonPluginContract.KEY_SUCCESS, false)) {
                return@withContext Result.error(
                    valuesResponse.getString(ZenLemonPluginContract.KEY_MESSAGE).orEmpty()
                        .ifBlank { "Plugin configuration values are unavailable" }
                )
            }

            val valuesJson = valuesResponse
                .getString(ZenLemonPluginContract.KEY_CONFIGURATION_VALUES_JSON)
                .orEmpty()
            val values = if (valuesJson.isBlank()) {
                JsonObject(emptyMap())
            } else {
                runCatching { json.decodeFromString<JsonObject>(valuesJson) }
                    .getOrElse { error ->
                        return@withContext Result.error(error.message ?: "Plugin configuration values are invalid")
                    }
            }
            Result.success(values)
        }

    suspend fun savePluginConfiguration(
        plugin: InstalledZenLemonPlugin,
        valuesJson: String
    ): PluginActionResult = withContext(Dispatchers.IO) {
        val response = runCatching {
            messengerClient.send(
                packageName = plugin.packageName,
                serviceClassName = plugin.serviceClassName,
                what = ZenLemonPluginContract.MSG_SET_CONFIGURATION_VALUES,
                data = Bundle().apply {
                    putString(ZenLemonPluginContract.KEY_CONFIGURATION_VALUES_JSON, valuesJson)
                },
                timeoutMillis = 60_000L
            )
        }.getOrElse { error ->
            return@withContext PluginActionResult(false, error.message ?: "Plugin settings could not be saved")
        }
        response.toPluginActionResult("Plugin settings saved")
    }

    suspend fun runPluginConfigurationAction(
        plugin: InstalledZenLemonPlugin,
        actionId: String
    ): PluginActionResult = withContext(Dispatchers.IO) {
        val response = runCatching {
            messengerClient.send(
                packageName = plugin.packageName,
                serviceClassName = plugin.serviceClassName,
                what = ZenLemonPluginContract.MSG_RUN_CONFIGURATION_ACTION,
                data = Bundle().apply {
                    putString(ZenLemonPluginContract.KEY_CONFIGURATION_ACTION_ID, actionId)
                },
                timeoutMillis = 120_000L
            )
        }.getOrElse { error ->
            return@withContext PluginActionResult(false, error.message ?: "Plugin action failed")
        }
        response.toPluginActionResult("Plugin action completed")
    }

    suspend fun preparePlaybackUrl(url: String): Result<Unit> =
        preparePlaybackStreamInfo(StreamInfo(url = url)).map { Unit }

    suspend fun preparePlaybackStreamInfo(streamInfo: StreamInfo): Result<StreamInfo> = withContext(Dispatchers.IO) {
        val url = streamInfo.url
        if (url.isBlank()) return@withContext Result.success(streamInfo)
        val plugins = discoverPlugins()
            .filter { it.enabled && it.manifest.hasCapability(ZenLemonPluginContract.CAPABILITY_PLAYBACK_PREPARE) }
        for (plugin in plugins) {
            val response = runCatching {
                messengerClient.send(
                    packageName = plugin.packageName,
                    serviceClassName = plugin.serviceClassName,
                    what = ZenLemonPluginContract.MSG_PREPARE_PLAYBACK,
                    data = Bundle().apply { putString(ZenLemonPluginContract.KEY_INPUT_URL, url) },
                    timeoutMillis = 120_000L
                )
            }.getOrNull() ?: continue

            if (!response.getBoolean(ZenLemonPluginContract.KEY_HANDLED, false)) continue
            if (response.getBoolean(ZenLemonPluginContract.KEY_SUCCESS, false)) {
                return@withContext Result.success(applyPlaybackPreparationResponse(streamInfo, response))
            }
            return@withContext Result.error(
                response.getString(ZenLemonPluginContract.KEY_MESSAGE).orEmpty()
                    .ifBlank { "${plugin.displayName} could not prepare playback" }
            )
        }
        Result.success(streamInfo)
    }

    suspend fun rewriteCastUrl(request: CastMediaRequest): String? = withContext(Dispatchers.IO) {
        val url = request.url
        if (url.isBlank()) return@withContext url
        val plugins = discoverPlugins()
            .filter { it.enabled && it.manifest.hasCapability(ZenLemonPluginContract.CAPABILITY_CAST_REWRITE_URL) }

        val deferredResponses = plugins.map { plugin ->
            async {
                runCatching {
                    messengerClient.send(
                        packageName = plugin.packageName,
                        serviceClassName = plugin.serviceClassName,
                        what = ZenLemonPluginContract.MSG_REWRITE_CAST_URL,
                        data = request.toCastRewriteBundle(),
                        timeoutMillis = 10_000L
                    )
                }.getOrNull()
            }
        }
        val responses = deferredResponses.awaitAll()

        for (response in responses) {
            if (response == null) continue
            if (!response.getBoolean(ZenLemonPluginContract.KEY_HANDLED, false)) continue
            if (!response.getBoolean(ZenLemonPluginContract.KEY_SUCCESS, false)) return@withContext null
            return@withContext response.getString(ZenLemonPluginContract.KEY_OUTPUT_URL).orEmpty()
                .ifBlank { url }
        }
        url
    }

    suspend fun rewriteCastUrl(url: String): String? =
        rewriteCastUrl(CastMediaRequest(url = url, title = "ZenLemon"))

    private fun applyPlaybackPreparationResponse(
        streamInfo: StreamInfo,
        response: Bundle
    ): StreamInfo {
        val outputUrl = response.getString(ZenLemonPluginContract.KEY_OUTPUT_URL).orEmpty()
            .ifBlank { streamInfo.url }
        val responseHeaders = parseHeadersJson(response.getString(ZenLemonPluginContract.KEY_HEADERS_JSON).orEmpty())
        val responseUserAgent = response.getString(ZenLemonPluginContract.KEY_USER_AGENT)
            ?.takeIf { it.isNotBlank() }
        val streamType = parsePluginStreamType(response.getString(ZenLemonPluginContract.KEY_STREAM_TYPE))
            ?: streamInfo.streamType
        val drmInfo = parsePluginDrmInfo(response.getString(ZenLemonPluginContract.KEY_DRM_JSON))
            ?: streamInfo.drmInfo
        return streamInfo.copy(
            url = outputUrl,
            headers = streamInfo.headers + responseHeaders,
            userAgent = responseUserAgent ?: streamInfo.userAgent,
            streamType = streamType,
            containerExtension = streamInfo.containerExtension ?: streamType.defaultContainerExtension(),
            drmInfo = drmInfo
        )
    }

    private fun CastMediaRequest.toCastRewriteBundle(): Bundle = Bundle().apply {
        putString(ZenLemonPluginContract.KEY_INPUT_URL, url)
        putString(ZenLemonPluginContract.KEY_STREAM_TYPE, mimeType.orEmpty())
        putString(ZenLemonPluginContract.KEY_HEADERS_JSON, headers.toHeadersJson())
        putString(ZenLemonPluginContract.KEY_USER_AGENT, userAgent.orEmpty())
        putBoolean(ZenLemonPluginContract.KEY_ALLOW_INVALID_SSL, allowInvalidSsl)
        putString(ZenLemonPluginContract.KEY_PROXY_HOST, proxyHost)
        proxyPort?.let { putInt(ZenLemonPluginContract.KEY_PROXY_PORT, it) }
        putString(ZenLemonPluginContract.KEY_CAST_REWRITE_REASON, rewriteRequiredReason?.name.orEmpty())
    }

    private fun Map<String, String>.toHeadersJson(): String {
        if (isEmpty()) return ""
        val jsonObject = JSONObject()
        forEach { (name, value) ->
            if (name.isNotBlank() && value.isNotBlank() && name.isSafeHttpHeaderName()) {
                jsonObject.put(name, value)
            }
        }
        return jsonObject.toString()
    }

    private fun parseHeadersJson(raw: String): Map<String, String> =
        runCatching {
            if (raw.isBlank()) return@runCatching emptyMap()
            val jsonObject = JSONObject(raw)
            val headers = linkedMapOf<String, String>()
            jsonObject.keys().forEach { key ->
                val value = jsonObject.optString(key).takeIf { it.isNotBlank() } ?: return@forEach
                if (key.isSafeHttpHeaderName()) headers[key] = value
            }
            headers
        }.getOrDefault(emptyMap())

    private fun parsePluginDrmInfo(raw: String?): DrmInfo? =
        runCatching {
            if (raw.isNullOrBlank()) return@runCatching null
            val jsonObject = JSONObject(raw)
            val scheme = parsePluginDrmScheme(jsonObject.optString("scheme")) ?: return@runCatching null
            val licenseUrl = jsonObject.optString("licenseUrl").ifBlank {
                jsonObject.optString("license_url")
            }
            if (licenseUrl.isBlank()) return@runCatching null
            DrmInfo(
                scheme = scheme,
                licenseUrl = licenseUrl,
                headers = parseHeadersJson(jsonObject.optJSONObject("headers")?.toString().orEmpty()),
                multiSession = jsonObject.optBoolean("multiSession", false),
                forceDefaultLicenseUrl = jsonObject.optBoolean("forceDefaultLicenseUrl", true),
                playClearContentWithoutKey = jsonObject.optBoolean("playClearContentWithoutKey", true)
            )
        }.getOrNull()

    private fun parsePluginDrmScheme(raw: String): DrmScheme? {
        val normalized = raw.trim().lowercase()
        return when {
            normalized == "widevine" || normalized == "com.widevine.alpha" -> DrmScheme.WIDEVINE
            normalized == "playready" || normalized == "com.microsoft.playready" -> DrmScheme.PLAYREADY
            normalized == "clearkey" || normalized == "org.w3.clearkey" -> DrmScheme.CLEARKEY
            else -> null
        }
    }

    private fun parsePluginStreamType(raw: String?): StreamType? {
        val normalized = raw.orEmpty().trim().lowercase()
        return when (normalized) {
            "dash", "mpd", "application/dash+xml" -> StreamType.DASH
            "smooth_streaming", "smoothstreaming", "smooth-streaming", "ss", "ism", "isml",
            "application/vnd.ms-sstr+xml" -> StreamType.SMOOTH_STREAMING
            "hls", "m3u8", "application/vnd.apple.mpegurl", "application/x-mpegurl" -> StreamType.HLS
            "mpeg_ts", "mpeg-ts", "ts" -> StreamType.MPEG_TS
            "progressive", "file" -> StreamType.PROGRESSIVE
            "rtsp", "rtsps" -> StreamType.RTSP
            else -> null
        }
    }

    private suspend fun syncPluginProvider(
        plugin: InstalledZenLemonPlugin,
        onProgress: (String) -> Unit
    ): PluginActionResult? {
        val providerResponse = runCatching {
            messengerClient.send(
                packageName = plugin.packageName,
                serviceClassName = plugin.serviceClassName,
                what = ZenLemonPluginContract.MSG_GET_PROVIDER_URL,
                timeoutMillis = 120_000L
            )
        }.getOrElse { error ->
            return PluginActionResult(false, error.message ?: "Plugin provider URL is unavailable")
        }
        if (!providerResponse.getBoolean(ZenLemonPluginContract.KEY_SUCCESS, false)) {
            return PluginActionResult(
                false,
                providerResponse.getString(ZenLemonPluginContract.KEY_MESSAGE).orEmpty()
                    .ifBlank { "Plugin provider URL is unavailable" }
            )
        }

        val providerUrl = providerResponse.getString(ZenLemonPluginContract.KEY_URL).orEmpty()
        if (providerUrl.isBlank()) {
            return PluginActionResult(false, "Plugin did not return a provider URL")
        }
        val providerName = providerResponse.getString(ZenLemonPluginContract.KEY_PROVIDER_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: plugin.manifest.providerName?.takeIf { it.isNotBlank() }
            ?: "${plugin.displayName} Plugin"
        val existingProvider = trackedProvider(plugin)
        val activeSource = combinedM3uRepository.getActiveLiveSource().first()

        val provider = if (existingProvider != null) {
            val updatedProvider = existingProvider.copy(
                name = providerName,
                serverUrl = providerUrl,
                m3uUrl = providerUrl,
                epgSyncMode = ProviderEpgSyncMode.BACKGROUND,
                m3uVodClassificationEnabled = false,
                isActive = true,
                lastSyncedAt = 0L
            )
            when (val updateResult = providerRepository.updateProvider(updatedProvider)) {
                is Result.Error -> return PluginActionResult(false, updateResult.message)
                Result.Loading -> return PluginActionResult(false, "Provider update is still running")
                is Result.Success -> Unit
            }
            when (val refreshResult = providerRepository.refreshProviderData(
                providerId = updatedProvider.id,
                force = true,
                epgSyncModeOverride = ProviderEpgSyncMode.BACKGROUND,
                onProgress = onProgress
            )) {
                is Result.Error -> return PluginActionResult(false, refreshResult.message)
                Result.Loading -> return PluginActionResult(false, "Provider refresh is still running")
                is Result.Success -> Unit
            }
            updatedProvider
        } else {
            when (val createResult = providerRepository.validateM3u(
                url = providerUrl,
                name = providerName,
                epgSyncMode = ProviderEpgSyncMode.BACKGROUND,
                m3uVodClassificationEnabled = false,
                onProgress = onProgress
            )) {
                is Result.Error -> return PluginActionResult(false, createResult.message)
                Result.Loading -> return PluginActionResult(false, "Provider sync is still running")
                is Result.Success -> createResult.data
            }
        }

        prefs.edit().putLong(providerKey(plugin.manifest.id), provider.id).apply()
        providerRepository.setActiveProvider(provider.id)
        attachProviderToLiveSource(provider.id, activeSource)
        refreshTvInputCatalogInBackground()
        return null
    }

    private suspend fun removePluginProvider(plugin: InstalledZenLemonPlugin): PluginActionResult? {
        val providerId = prefs.getLong(providerKey(plugin.manifest.id), -1L).takeIf { it > 0L }
            ?: return null
        when (val result = providerRepository.deleteProvider(providerId)) {
            is Result.Error -> return PluginActionResult(false, result.message)
            Result.Loading -> return PluginActionResult(false, "Provider removal is still running")
            is Result.Success -> Unit
        }
        val activeSource = combinedM3uRepository.getActiveLiveSource().first()
        if (activeSource is ActiveLiveSource.ProviderSource && activeSource.providerId == providerId) {
            combinedM3uRepository.setActiveLiveSource(null)
        }
        prefs.edit().remove(providerKey(plugin.manifest.id)).apply()
        refreshTvInputCatalogInBackground()
        return null
    }

    private suspend fun attachProviderToLiveSource(providerId: Long, activeSource: ActiveLiveSource?) {
        when (activeSource) {
            is ActiveLiveSource.CombinedM3uSource -> {
                combinedM3uRepository.addProvider(activeSource.profileId, providerId)
                combinedM3uRepository.setActiveLiveSource(activeSource)
            }
            is ActiveLiveSource.ProviderSource,
            null -> combinedM3uRepository.setActiveLiveSource(ActiveLiveSource.ProviderSource(providerId))
        }
    }

    private suspend fun trackedProvider(plugin: InstalledZenLemonPlugin): Provider? {
        val providerId = prefs.getLong(providerKey(plugin.manifest.id), -1L).takeIf { it > 0L }
            ?: return providerRepository.getProviders().first().firstOrNull {
                it.type == ProviderType.M3U && it.m3uUrl.isNotBlank() && it.name == plugin.manifest.providerName
            }
        return providerRepository.getProvider(providerId)
    }

    private fun refreshTvInputCatalogInBackground() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).let { scope ->
            scope.launchCatching { tvInputChannelSyncManager.refreshTvInputCatalog() }
        }
    }

    private fun resolvePlugin(resolveInfo: ResolveInfo): InstalledZenLemonPlugin? {
        val serviceInfo = resolveInfo.serviceInfo ?: return null
        val packageName = serviceInfo.packageName ?: return null
        val serviceName = serviceInfo.name ?: return null
        val appLabel = serviceInfo.loadLabel(context.packageManager)?.toString().orEmpty()
        val manifest = readManifestFromService(packageName, serviceName)
            ?: readManifestFromMetadata(serviceInfo.metaData)
            ?: ZenLemonPluginManifest(
                id = packageName,
                name = appLabel.ifBlank { packageName },
                description = "ZenLemon plugin"
            )
        val status = runCatching {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                messengerClient.send(
                    packageName = packageName,
                    serviceClassName = serviceName,
                    what = ZenLemonPluginContract.MSG_GET_STATUS,
                    timeoutMillis = 2_500L
                )
            }
        }.getOrNull()
        return InstalledZenLemonPlugin(
            packageName = packageName,
            serviceClassName = serviceName,
            appLabel = appLabel,
            manifest = manifest,
            enabled = prefs.getBoolean(enabledKey(manifest.id), false),
            statusLabel = status?.getString(ZenLemonPluginContract.KEY_STATUS_LABEL).orEmpty(),
            lastMessage = status?.getString(ZenLemonPluginContract.KEY_MESSAGE).orEmpty()
        )
    }

    private fun readManifestFromService(packageName: String, serviceName: String): ZenLemonPluginManifest? =
        runCatching {
            val response = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                messengerClient.send(
                    packageName = packageName,
                    serviceClassName = serviceName,
                    what = ZenLemonPluginContract.MSG_GET_MANIFEST,
                    timeoutMillis = 3_000L
                )
            }
            val manifestJson = response.getString(ZenLemonPluginContract.KEY_MANIFEST_JSON).orEmpty()
            json.decodeFromString<ZenLemonPluginManifest>(manifestJson)
        }.getOrNull()

    private fun readManifestFromMetadata(metaData: Bundle?): ZenLemonPluginManifest? {
        if (metaData == null) return null

        val manifestJson = metaData.metaString(ZenLemonPluginContract.META_MANIFEST_JSON)
        if (manifestJson.isNotBlank()) {
            runCatching { json.decodeFromString<ZenLemonPluginManifest>(manifestJson) }
                .getOrNull()
                ?.let { return it }
        }

        val id = metaData.metaString(ZenLemonPluginContract.META_ID).takeIf { it.isNotBlank() }
            ?: return null
        val name = metaData.metaString(ZenLemonPluginContract.META_NAME).ifBlank { id }
        return ZenLemonPluginManifest(
            id = id,
            name = name,
            versionName = metaData.metaString(ZenLemonPluginContract.META_VERSION_NAME),
            versionCode = metaData.metaLong(ZenLemonPluginContract.META_VERSION_CODE),
            description = metaData.metaString(ZenLemonPluginContract.META_DESCRIPTION),
            capabilities = metaData.metaCsv(ZenLemonPluginContract.META_CAPABILITIES),
            configurationMode = metaData.metaString(ZenLemonPluginContract.META_CONFIGURATION_MODE)
                .takeIf { it.isNotBlank() },
            configurationActivityAction = metaData
                .metaString(ZenLemonPluginContract.META_CONFIGURATION_ACTIVITY_ACTION)
                .takeIf { it.isNotBlank() },
            providerName = metaData.metaString(ZenLemonPluginContract.META_PROVIDER_NAME)
                .takeIf { it.isNotBlank() }
        )
    }

    private fun queryPluginServices(): List<ResolveInfo> {
        val intent = Intent(ZenLemonPluginContract.ACTION_PLUGIN_SERVICE)
        val packageManager = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentServices(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA)
        }
    }

    private fun launchPackageInstaller(apkFile: File): Result<Unit> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            return Result.error("Allow installs from ZenLemon, then choose the plugin APK again")
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return try {
            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (error: ActivityNotFoundException) {
            Result.error("No package installer is available on this device", error)
        } catch (error: SecurityException) {
            Result.error("The package installer could not be launched", error)
        }
    }

    private fun pluginApkFile(fileName: String): File {
        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.cacheDir, "downloads")
        return File(downloadsDir, "plugin-apks/$fileName")
    }

    private fun isHttpOrHttpsUrl(value: String): Boolean =
        runCatching {
            val parsed = URI(value)
            parsed.scheme.equals("http", ignoreCase = true) ||
                parsed.scheme.equals("https", ignoreCase = true)
        }.getOrDefault(false)

    private fun enabledKey(pluginId: String): String = "enabled.$pluginId"
    private fun providerKey(pluginId: String): String = "provider.$pluginId"
}

private fun kotlinx.coroutines.CoroutineScope.launchCatching(block: suspend () -> Unit) {
    launch { runCatching { block() } }
}

private fun StreamType.defaultContainerExtension(): String? = when (this) {
    StreamType.DASH -> "mpd"
    StreamType.SMOOTH_STREAMING -> "ism"
    StreamType.HLS -> "m3u8"
    StreamType.MPEG_TS -> "ts"
    StreamType.PROGRESSIVE,
    StreamType.RTSP,
    StreamType.UNKNOWN -> null
}

private fun String.isSafeHttpHeaderName(): Boolean =
    isNotBlank() && all { char ->
        char.isLetterOrDigit() || char in setOf('!', '#', '$', '%', '&', '\'', '*', '+', '.', '^', '_', '`', '|', '~', '-')
    }

@Suppress("DEPRECATION")
private fun Bundle.metaString(key: String): String = when (val value = get(key)) {
    is String -> value
    is CharSequence -> value.toString()
    is Number -> value.toString()
    is Boolean -> value.toString()
    else -> ""
}

@Suppress("DEPRECATION")
private fun Bundle.metaLong(key: String): Long = when (val value = get(key)) {
    is Long -> value
    is Int -> value.toLong()
    is Short -> value.toLong()
    is Byte -> value.toLong()
    is String -> value.toLongOrNull() ?: 0L
    else -> 0L
}

private fun Bundle.metaCsv(key: String): List<String> =
    metaString(key)
        .split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }

private fun Bundle.toPluginActionResult(successMessage: String): PluginActionResult {
    val success = getBoolean(ZenLemonPluginContract.KEY_SUCCESS, false)
    return PluginActionResult(
        success = success,
        message = getString(ZenLemonPluginContract.KEY_MESSAGE).orEmpty()
            .ifBlank { if (success) successMessage else "Plugin operation failed" }
    )
}
