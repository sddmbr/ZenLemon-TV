package com.zenlemon.app.di

import com.zenlemon.data.local.DatabaseTransactionRunner
import com.zenlemon.data.local.RoomDatabaseTransactionRunner
import com.zenlemon.data.manager.DownloadManagerImpl
import com.zenlemon.data.preferences.PreferencesRepository
import com.zenlemon.data.security.AndroidKeystoreCredentialCrypto
import com.zenlemon.data.security.CredentialCrypto
import com.zenlemon.data.sync.ProviderSyncStateReaderImpl
import com.zenlemon.data.validation.ProviderSetupInputValidatorImpl
import com.zenlemon.domain.manager.ParentalPinVerifier
import com.zenlemon.domain.manager.ProviderSetupInputValidator
import com.zenlemon.domain.manager.ProviderSyncStateReader
import com.zenlemon.data.repository.*
import com.zenlemon.domain.manager.ParentalControlSessionStore
import com.zenlemon.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindProviderRepository(impl: ProviderRepositoryImpl): ProviderRepository

    @Binds @Singleton
    abstract fun bindChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository

    @Binds @Singleton
    abstract fun bindCombinedM3uRepository(impl: CombinedM3uRepositoryImpl): CombinedM3uRepository

    @Binds @Singleton
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository

    @Binds @Singleton
    abstract fun bindSeriesRepository(impl: SeriesRepositoryImpl): SeriesRepository

    @Binds @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds @Singleton
    abstract fun bindEpgRepository(impl: EpgRepositoryImpl): EpgRepository

    @Binds @Singleton
    abstract fun bindEpgSourceRepository(impl: EpgSourceRepositoryImpl): EpgSourceRepository

    @Binds @Singleton
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository

    @Binds @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds @Singleton
    abstract fun bindPlaybackHistoryRepository(impl: PlaybackHistoryRepositoryImpl): PlaybackHistoryRepository

    @Binds @Singleton
    abstract fun bindExternalRatingsRepository(impl: ExternalRatingsRepositoryImpl): ExternalRatingsRepository

    @Binds @Singleton
    abstract fun bindSyncMetadataRepository(impl: SyncMetadataRepositoryImpl): SyncMetadataRepository

    @Binds @Singleton
    abstract fun bindPlaybackCompatibilityRepository(impl: PlaybackCompatibilityRepositoryImpl): PlaybackCompatibilityRepository

    @Binds @Singleton
    abstract fun bindDatabaseTransactionRunner(impl: RoomDatabaseTransactionRunner): DatabaseTransactionRunner

    @Binds @Singleton
    abstract fun bindBackupManager(impl: com.zenlemon.data.manager.BackupManagerImpl): com.zenlemon.domain.manager.BackupManager

    @Binds @Singleton
    abstract fun bindDriveBackupSyncManager(impl: com.zenlemon.data.manager.GoogleDriveBackupSyncManager): com.zenlemon.domain.manager.DriveBackupSyncManager

    @Binds @Singleton
    abstract fun bindRecordingManager(impl: com.zenlemon.data.manager.RecordingManagerImpl): com.zenlemon.domain.manager.RecordingManager

    @Binds @Singleton
    abstract fun bindDownloadManager(impl: DownloadManagerImpl): DownloadManager

    @Binds @Singleton
    abstract fun bindProgramReminderManager(impl: com.zenlemon.data.manager.ProgramReminderManagerImpl): com.zenlemon.domain.manager.ProgramReminderManager

    @Binds @Singleton
    abstract fun bindParentalControlSessionStore(impl: PreferencesRepository): ParentalControlSessionStore

    @Binds @Singleton
    abstract fun bindParentalPinVerifier(impl: PreferencesRepository): ParentalPinVerifier

    @Binds @Singleton
    abstract fun bindProviderSetupInputValidator(impl: ProviderSetupInputValidatorImpl): ProviderSetupInputValidator

    @Binds @Singleton
    abstract fun bindProviderSyncStateReader(impl: ProviderSyncStateReaderImpl): ProviderSyncStateReader

    @Binds @Singleton
    abstract fun bindCredentialCrypto(impl: AndroidKeystoreCredentialCrypto): CredentialCrypto

    companion object {
        @Provides
        @Singleton
        fun provideRepositoryCoroutineScope(): CoroutineScope {
            return CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }

        @Provides
        @Singleton
        fun provideM3uParser(): com.zenlemon.data.parser.M3uParser {
            return com.zenlemon.data.parser.M3uParser()
        }
    }
}
