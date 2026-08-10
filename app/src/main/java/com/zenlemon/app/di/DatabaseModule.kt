package com.zenlemon.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.zenlemon.app.BuildConfig
import com.zenlemon.data.local.ZenLemonDatabase
import com.zenlemon.data.local.dao.*
import com.zenlemon.data.remote.jellyfin.JellyfinProvider
import com.google.gson.Gson
import okhttp3.OkHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val DEBUG_SLOW_QUERY_THRESHOLD_MS = 100L

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ZenLemonDatabase =
        Room.databaseBuilder(
            context,
            ZenLemonDatabase::class.java,
            "zenlemon.db"
        )
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .openHelperFactory(
                if (BuildConfig.DEBUG) {
                    SlowQueryLoggingOpenHelperFactory(
                        delegate = FrameworkSQLiteOpenHelperFactory(),
                        slowQueryThresholdMs = DEBUG_SLOW_QUERY_THRESHOLD_MS
                    )
                } else {
                    FrameworkSQLiteOpenHelperFactory()
                }
            )
            .addMigrations(
                ZenLemonDatabase.MIGRATION_1_2,
                ZenLemonDatabase.MIGRATION_2_3,
                ZenLemonDatabase.MIGRATION_3_4,
                ZenLemonDatabase.MIGRATION_4_5,
                ZenLemonDatabase.MIGRATION_5_6,
                ZenLemonDatabase.MIGRATION_6_7,
                ZenLemonDatabase.MIGRATION_7_8,
                ZenLemonDatabase.MIGRATION_8_9,
                ZenLemonDatabase.MIGRATION_9_10,
                ZenLemonDatabase.MIGRATION_10_11,
                ZenLemonDatabase.MIGRATION_11_12,
                ZenLemonDatabase.MIGRATION_12_13,
                ZenLemonDatabase.MIGRATION_13_14,
                ZenLemonDatabase.MIGRATION_14_15,
                ZenLemonDatabase.MIGRATION_15_16,
                ZenLemonDatabase.MIGRATION_16_17,
                ZenLemonDatabase.MIGRATION_17_18,
                ZenLemonDatabase.MIGRATION_18_19,
                ZenLemonDatabase.MIGRATION_19_20,
                ZenLemonDatabase.MIGRATION_20_21,
                ZenLemonDatabase.MIGRATION_21_22,
                ZenLemonDatabase.MIGRATION_22_23,
                ZenLemonDatabase.MIGRATION_23_24,
                ZenLemonDatabase.MIGRATION_24_25,
                ZenLemonDatabase.MIGRATION_25_26,
                ZenLemonDatabase.MIGRATION_26_27,
                ZenLemonDatabase.MIGRATION_27_28,
                ZenLemonDatabase.MIGRATION_28_29,
                ZenLemonDatabase.MIGRATION_29_30,
                ZenLemonDatabase.MIGRATION_30_31,
                ZenLemonDatabase.MIGRATION_31_32,
                ZenLemonDatabase.MIGRATION_32_33,
                ZenLemonDatabase.MIGRATION_33_34,
                ZenLemonDatabase.MIGRATION_34_35,
                ZenLemonDatabase.MIGRATION_35_36,
                ZenLemonDatabase.MIGRATION_36_37,
                ZenLemonDatabase.MIGRATION_37_38,
                ZenLemonDatabase.MIGRATION_38_39,
                ZenLemonDatabase.MIGRATION_39_40,
                ZenLemonDatabase.MIGRATION_40_41,
                ZenLemonDatabase.MIGRATION_41_42,
                ZenLemonDatabase.MIGRATION_42_43,
                ZenLemonDatabase.MIGRATION_43_44,
                ZenLemonDatabase.MIGRATION_44_45,
                ZenLemonDatabase.MIGRATION_45_46,
                ZenLemonDatabase.MIGRATION_46_47,
                ZenLemonDatabase.MIGRATION_47_48,
                ZenLemonDatabase.MIGRATION_48_49,
                ZenLemonDatabase.MIGRATION_49_50,
                ZenLemonDatabase.MIGRATION_50_51,
                ZenLemonDatabase.MIGRATION_51_52,
                ZenLemonDatabase.MIGRATION_52_53,
                ZenLemonDatabase.MIGRATION_53_54,
                ZenLemonDatabase.MIGRATION_54_55,
                ZenLemonDatabase.MIGRATION_55_56,
                ZenLemonDatabase.MIGRATION_56_57,
                ZenLemonDatabase.MIGRATION_57_58,
                ZenLemonDatabase.MIGRATION_58_59,
                ZenLemonDatabase.MIGRATION_59_60,
                ZenLemonDatabase.MIGRATION_60_61,
                ZenLemonDatabase.MIGRATION_61_62
            )
            // NOTE: fallbackToDestructiveMigration() intentionally removed.
            // All future schema changes MUST add a corresponding Migration in ZenLemonDatabase.
            .build()

    @Provides @Singleton
    fun provideJellyfinProvider(okHttpClient: OkHttpClient, gson: Gson): JellyfinProvider = JellyfinProvider(okHttpClient, gson)

    @Provides fun provideProviderDao(db: ZenLemonDatabase): ProviderDao = db.providerDao()
    @Provides fun provideChannelDao(db: ZenLemonDatabase): ChannelDao = db.channelDao()
    @Provides fun provideChannelPreferenceDao(db: ZenLemonDatabase): ChannelPreferenceDao = db.channelPreferenceDao()
    @Provides fun provideMovieDao(db: ZenLemonDatabase): MovieDao = db.movieDao()
    @Provides fun provideSeriesDao(db: ZenLemonDatabase): SeriesDao = db.seriesDao()
    @Provides fun provideEpisodeDao(db: ZenLemonDatabase): EpisodeDao = db.episodeDao()
    @Provides fun provideCategoryDao(db: ZenLemonDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideCatalogSyncDao(db: ZenLemonDatabase): CatalogSyncDao = db.catalogSyncDao()
    @Provides fun provideProgramDao(db: ZenLemonDatabase): ProgramDao = db.programDao()
    @Provides fun provideFavoriteDao(db: ZenLemonDatabase): FavoriteDao = db.favoriteDao()
    @Provides fun provideVirtualGroupDao(db: ZenLemonDatabase): VirtualGroupDao = db.virtualGroupDao()
    @Provides fun providePlaybackHistoryDao(db: ZenLemonDatabase): PlaybackHistoryDao = db.playbackHistoryDao()
    @Provides fun provideTmdbIdentityDao(db: ZenLemonDatabase): TmdbIdentityDao = db.tmdbIdentityDao()
    @Provides fun provideSearchHistoryDao(db: ZenLemonDatabase): SearchHistoryDao = db.searchHistoryDao()
    @Provides fun provideSearchDao(db: ZenLemonDatabase): SearchDao = db.searchDao()
    @Provides fun provideSyncMetadataDao(db: ZenLemonDatabase): SyncMetadataDao = db.syncMetadataDao()
    @Provides fun provideMovieCategoryHydrationDao(db: ZenLemonDatabase): MovieCategoryHydrationDao = db.movieCategoryHydrationDao()
    @Provides fun provideSeriesCategoryHydrationDao(db: ZenLemonDatabase): SeriesCategoryHydrationDao = db.seriesCategoryHydrationDao()
    @Provides fun provideEpgSourceDao(db: ZenLemonDatabase): EpgSourceDao = db.epgSourceDao()
    @Provides fun provideProviderEpgSourceDao(db: ZenLemonDatabase): ProviderEpgSourceDao = db.providerEpgSourceDao()
    @Provides fun provideEpgChannelDao(db: ZenLemonDatabase): EpgChannelDao = db.epgChannelDao()
    @Provides fun provideEpgProgrammeDao(db: ZenLemonDatabase): EpgProgrammeDao = db.epgProgrammeDao()
    @Provides fun provideChannelEpgMappingDao(db: ZenLemonDatabase): ChannelEpgMappingDao = db.channelEpgMappingDao()
    @Provides fun provideCombinedM3uProfileDao(db: ZenLemonDatabase): CombinedM3uProfileDao = db.combinedM3uProfileDao()
    @Provides fun provideCombinedM3uProfileMemberDao(db: ZenLemonDatabase): CombinedM3uProfileMemberDao = db.combinedM3uProfileMemberDao()
    @Provides fun provideRecordingScheduleDao(db: ZenLemonDatabase): RecordingScheduleDao = db.recordingScheduleDao()
    @Provides fun provideRecordingRunDao(db: ZenLemonDatabase): RecordingRunDao = db.recordingRunDao()
    @Provides fun provideProgramReminderDao(db: ZenLemonDatabase): ProgramReminderDao = db.programReminderDao()
    @Provides fun provideRecordingStorageDao(db: ZenLemonDatabase): RecordingStorageDao = db.recordingStorageDao()
    @Provides fun providePlaybackCompatibilityDao(db: ZenLemonDatabase): PlaybackCompatibilityDao = db.playbackCompatibilityDao()
    @Provides fun provideXtreamContentIndexDao(db: ZenLemonDatabase): XtreamContentIndexDao = db.xtreamContentIndexDao()
    @Provides fun provideXtreamIndexJobDao(db: ZenLemonDatabase): XtreamIndexJobDao = db.xtreamIndexJobDao()
    @Provides fun provideXtreamLiveOnboardingDao(db: ZenLemonDatabase): XtreamLiveOnboardingDao = db.xtreamLiveOnboardingDao()
    @Provides fun provideDownloadDao(db: ZenLemonDatabase): DownloadDao = db.downloadDao()
}
