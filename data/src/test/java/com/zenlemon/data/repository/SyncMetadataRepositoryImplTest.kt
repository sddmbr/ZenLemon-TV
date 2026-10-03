package com.zenlemon.data.repository

import com.google.common.truth.Truth.assertThat
import com.zenlemon.data.local.dao.SyncMetadataDao
import com.zenlemon.data.local.entity.SyncMetadataEntity
import com.zenlemon.domain.model.SyncMetadata
import com.zenlemon.domain.model.VodSyncMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SyncMetadataRepositoryImplTest {

    private val dao: SyncMetadataDao = mock()
    private val repository = SyncMetadataRepositoryImpl(dao)

    @Test
    fun `observeMetadata maps and returns entity from dao`() = runTest {
        val providerId = 1L
        val entity = SyncMetadataEntity(
            providerId = providerId,
            lastLiveSync = 1000L,
            lastLiveSuccess = 1000L,
            lastMovieSync = 2000L,
            lastSeriesSync = 3000L,
            lastSeriesSuccess = 3000L,
            lastEpgSync = 4000L,
            lastEpgSuccess = 4000L,
            lastMovieAttempt = 2000L,
            lastMovieSuccess = 2000L,
            lastMoviePartial = 0L,
            liveCount = 10,
            movieCount = 20,
            seriesCount = 30,
            epgCount = 40,
            lastSyncStatus = "SUCCESS",
            movieSyncMode = "FULL",
            movieWarningsCount = 0,
            movieCatalogStale = false,
            liveAvoidFullUntil = 0L,
            movieAvoidFullUntil = 0L,
            seriesAvoidFullUntil = 0L,
            liveSequentialFailuresRemembered = false,
            liveHealthySyncStreak = 5,
            movieParallelFailuresRemembered = false,
            movieHealthySyncStreak = 5,
            seriesSequentialFailuresRemembered = false,
            seriesHealthySyncStreak = 5
        )

        whenever(dao.get(providerId)).thenReturn(flowOf(entity))

        val result = repository.observeMetadata(providerId).first()

        assertThat(result).isNotNull()
        assertThat(result?.providerId).isEqualTo(providerId)
        assertThat(result?.lastLiveSync).isEqualTo(1000L)
        assertThat(result?.movieSyncMode).isEqualTo(VodSyncMode.FULL)
    }

    @Test
    fun `observeMetadata returns null when dao returns null`() = runTest {
        val providerId = 1L
        whenever(dao.get(providerId)).thenReturn(flowOf(null))

        val result = repository.observeMetadata(providerId).first()

        assertThat(result).isNull()
    }

    @Test
    fun `getMetadata maps and returns entity from dao`() = runTest {
        val providerId = 1L
        val entity = SyncMetadataEntity(
            providerId = providerId,
            lastLiveSync = 1000L,
            lastLiveSuccess = 1000L,
            lastMovieSync = 2000L,
            lastSeriesSync = 3000L,
            lastSeriesSuccess = 3000L,
            lastEpgSync = 4000L,
            lastEpgSuccess = 4000L,
            lastMovieAttempt = 2000L,
            lastMovieSuccess = 2000L,
            lastMoviePartial = 0L,
            liveCount = 10,
            movieCount = 20,
            seriesCount = 30,
            epgCount = 40,
            lastSyncStatus = "SUCCESS",
            movieSyncMode = "FULL",
            movieWarningsCount = 0,
            movieCatalogStale = false,
            liveAvoidFullUntil = 0L,
            movieAvoidFullUntil = 0L,
            seriesAvoidFullUntil = 0L,
            liveSequentialFailuresRemembered = false,
            liveHealthySyncStreak = 5,
            movieParallelFailuresRemembered = false,
            movieHealthySyncStreak = 5,
            seriesSequentialFailuresRemembered = false,
            seriesHealthySyncStreak = 5
        )

        whenever(dao.getSync(providerId)).thenReturn(entity)

        val result = repository.getMetadata(providerId)

        assertThat(result).isNotNull()
        assertThat(result?.providerId).isEqualTo(providerId)
        assertThat(result?.lastLiveSync).isEqualTo(1000L)
        assertThat(result?.movieSyncMode).isEqualTo(VodSyncMode.FULL)
    }

    @Test
    fun `getMetadata returns null when dao returns null`() = runTest {
        val providerId = 1L
        whenever(dao.getSync(providerId)).thenReturn(null)

        val result = repository.getMetadata(providerId)

        assertThat(result).isNull()
    }

    @Test
    fun `updateMetadata delegates to dao insertOrUpdate`() = runTest {
        val metadata = SyncMetadata(
            providerId = 1L,
            lastLiveSync = 1000L,
            lastLiveSuccess = 1000L,
            lastMovieSync = 2000L,
            lastSeriesSync = 3000L,
            lastSeriesSuccess = 3000L,
            lastEpgSync = 4000L,
            lastEpgSuccess = 4000L,
            lastMovieAttempt = 2000L,
            lastMovieSuccess = 2000L,
            lastMoviePartial = 0L,
            liveCount = 10,
            movieCount = 20,
            seriesCount = 30,
            epgCount = 40,
            lastSyncStatus = "SUCCESS",
            movieSyncMode = VodSyncMode.FULL,
            movieWarningsCount = 0,
            movieCatalogStale = false,
            liveAvoidFullUntil = 0L,
            movieAvoidFullUntil = 0L,
            seriesAvoidFullUntil = 0L,
            liveSequentialFailuresRemembered = false,
            liveHealthySyncStreak = 5,
            movieParallelFailuresRemembered = false,
            movieHealthySyncStreak = 5,
            seriesSequentialFailuresRemembered = false,
            seriesHealthySyncStreak = 5
        )

        repository.updateMetadata(metadata)

        val captor = argumentCaptor<SyncMetadataEntity>()
        verify(dao).insertOrUpdate(captor.capture())

        val capturedEntity = captor.firstValue
        assertThat(capturedEntity.providerId).isEqualTo(1L)
        assertThat(capturedEntity.lastLiveSync).isEqualTo(1000L)
        assertThat(capturedEntity.movieSyncMode).isEqualTo("FULL")
    }

    @Test
    fun `clearMetadata delegates to dao delete`() = runTest {
        val providerId = 1L

        repository.clearMetadata(providerId)

        verify(dao).delete(providerId)
    }
}
