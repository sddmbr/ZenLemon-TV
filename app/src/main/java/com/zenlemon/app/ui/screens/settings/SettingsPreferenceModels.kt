package com.zenlemon.app.ui.screens.settings

import android.app.Application
import com.zenlemon.app.R
import com.zenlemon.app.ui.model.LiveTvChannelMode
import com.zenlemon.app.ui.model.LiveTvQuickFilterVisibilityMode
import com.zenlemon.app.ui.model.VodViewMode
import com.zenlemon.domain.model.AppTimeFormat
import com.zenlemon.domain.model.AppHomeDashboardShelf
import com.zenlemon.domain.model.AppLandingDestination
import com.zenlemon.domain.model.AppTopLevelDestination
import com.zenlemon.domain.model.AudioOutputPreference
import com.zenlemon.domain.model.Category
import com.zenlemon.domain.model.ExternalPlaybackMode
import com.zenlemon.domain.model.ChannelNumberingMode
import com.zenlemon.domain.model.DecoderMode
import com.zenlemon.domain.model.GroupedChannelLabelMode
import com.zenlemon.domain.model.LiveChannelGroupingMode
import com.zenlemon.domain.model.LiveVariantPreferenceMode
import com.zenlemon.domain.model.PlaybackBufferMode
import com.zenlemon.domain.model.VodDuplicateHandlingMode
import com.zenlemon.domain.model.VodHttpProtocolMode
import com.zenlemon.domain.model.VodVariantPreferenceMode
import com.zenlemon.domain.model.PlayerSurfaceMode
import com.zenlemon.domain.model.Provider
import com.zenlemon.domain.model.RemoteShortcutPreferences
import com.zenlemon.domain.model.TimeshiftBackendPreference

enum class ProviderWarningAction {
    EPG,
    MOVIES,
    SERIES
}

enum class ProviderSyncSelection {
    SYNC_NOW,
    REBUILD_INDEX,
    TV,
    MOVIES,
    SERIES,
    EPG
}

internal data class SettingsPreferenceSnapshot(
    val providers: List<Provider>,
    val activeProviderId: Long?,
    val parentalControlLevel: Int,
    val hasParentalPin: Boolean,
    val appLanguage: String,
    val appLandingDestination: AppLandingDestination,
    val appTopLevelDestinations: List<AppTopLevelDestination>,
    val appHomeDashboardShelves: List<AppHomeDashboardShelf>,
    val appTimeFormat: AppTimeFormat,
    val preferredAudioLanguage: String,
    val playerMediaSessionEnabled: Boolean,
    val playerFastRetryOnTransientFailures: Boolean,
    val playerAudioDecoderMode: DecoderMode,
    val playerVideoDecoderMode: DecoderMode,
    val playerPlaybackBufferMode: PlaybackBufferMode,
    val playerAudioOutputPreference: AudioOutputPreference,
    val playerCompatibilityMemoryEnabled: Boolean,
    val playerSurfaceMode: PlayerSurfaceMode,
    val playerVodHttpProtocolMode: VodHttpProtocolMode,
    val playerPlaybackSpeed: Float,
    val playerExternalPlaybackMode: ExternalPlaybackMode,
    val playerAudioVideoSyncEnabled: Boolean,
    val playerAudioVideoOffsetMs: Int,
    val centerTwoSlotMultiviewLayout: Boolean,
    val multiViewRespectProviderConnectionLimit: Boolean,
    val playerControlsTimeoutSeconds: Int,
    val playerLiveOverlayTimeoutSeconds: Int,
    val playerNoticeTimeoutSeconds: Int,
    val playerDiagnosticsTimeoutSeconds: Int,
    val subtitleTextScale: Float,
    val subtitleTextColor: Int,
    val subtitleBackgroundColor: Int,
    val playerLiveTranslationEnabled: Boolean,
    val playerLiveTranslationEndpoint: String,
    val wifiMaxVideoHeight: Int?,
    val ethernetMaxVideoHeight: Int?,
    val playerTimeshiftEnabled: Boolean,
    val playerTimeshiftDepthMinutes: Int,
    val playerTimeshiftBackend: TimeshiftBackendPreference,
    val defaultStopPlaybackTimerMinutes: Int,
    val defaultIdleStandbyTimerMinutes: Int,
    val lastSpeedTestMegabits: Double?,
    val lastSpeedTestTimestamp: Long?,
    val lastSpeedTestTransport: String?,
    val lastSpeedTestRecommendedHeight: Int?,
    val lastSpeedTestEstimated: Boolean,
    val isIncognitoMode: Boolean,
    val useXtreamTextClassification: Boolean,
    val xtreamBase64TextCompatibility: Boolean,
    val liveTvChannelMode: LiveTvChannelMode,
    val showLiveSourceSwitcher: Boolean,
    val showFavoritesCategory: Boolean,
    val showAllChannelsCategory: Boolean,
    val showRecentChannelsCategory: Boolean,
    val remoteShortcutPreferences: RemoteShortcutPreferences,
    val liveTvCategoryFilters: List<String>,
    val liveTvQuickFilterVisibilityMode: LiveTvQuickFilterVisibilityMode,
    val hideDecorativeLiveRows: Boolean,
    val liveChannelNumberingMode: ChannelNumberingMode,
    val liveChannelGroupingMode: LiveChannelGroupingMode,
    val groupedChannelLabelMode: GroupedChannelLabelMode,
    val liveVariantPreferenceMode: LiveVariantPreferenceMode,
    val vodViewMode: VodViewMode,
    val vodInfiniteScroll: Boolean,
    val vodDuplicateHandlingMode: VodDuplicateHandlingMode,
    val vodVariantPreferenceMode: VodVariantPreferenceMode,
    val guideDefaultCategoryId: Long,
    val guideDefaultCategoryOptions: List<Category>,
    val preventStandbyDuringPlayback: Boolean,
    val zapAutoRevert: Boolean,
    val autoPlayNextEpisode: Boolean,
    val autoCheckAppUpdates: Boolean,
    val autoDownloadAppUpdates: Boolean,
    val lastAppUpdateCheckAt: Long?,
    val cachedAppUpdateVersionName: String?,
    val cachedAppUpdateVersionCode: Int?,
    val cachedAppUpdateReleaseUrl: String?,
    val cachedAppUpdateDownloadUrl: String?,
    val cachedAppUpdateDownloadSha256: String?,
    val cachedAppUpdateReleaseNotes: String,
    val cachedAppUpdatePublishedAt: String?
)

internal fun ProviderSyncSelection.label(application: Application): String = when (this) {
    ProviderSyncSelection.SYNC_NOW -> application.getString(R.string.settings_sync_option_sync_now)
    ProviderSyncSelection.REBUILD_INDEX -> application.getString(R.string.settings_sync_option_rebuild_index)
    ProviderSyncSelection.TV -> application.getString(R.string.settings_sync_option_tv)
    ProviderSyncSelection.MOVIES -> application.getString(R.string.settings_sync_option_movies)
    ProviderSyncSelection.SERIES -> application.getString(R.string.settings_sync_option_series)
    ProviderSyncSelection.EPG -> application.getString(R.string.settings_sync_option_epg)
}
