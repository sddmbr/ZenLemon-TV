package com.zenlemon.domain.repository

import com.google.common.truth.Truth.assertThat
import com.zenlemon.domain.model.PlaybackCompatibilityKey
import com.zenlemon.domain.model.PlaybackCompatibilityRecord
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.TimeUnit

class PlaybackCompatibilityRepositoryTest {

    @Test
    fun `DEFAULT_RETENTION_MS is exactly 90 days`() {
        val expected = TimeUnit.DAYS.toMillis(90)
        assertThat(PlaybackCompatibilityRepository.DEFAULT_RETENTION_MS).isEqualTo(expected)
    }

    @Test
    fun `recordFailure uses current time as default`() = runTest {
        val fake = FakeRepository()
        val repository: PlaybackCompatibilityRepository = fake
        val key = PlaybackCompatibilityKey(
            deviceFingerprint = "fp",
            deviceModel = "model",
            androidSdk = 30,
            streamType = "HLS",
            videoMimeType = "video/avc",
            resolutionBucket = "1080P",
            decoderName = "dec",
            surfaceType = "surf"
        )

        val before = System.currentTimeMillis()
        repository.recordFailure(key, "stall")
        val after = System.currentTimeMillis()

        assertThat(fake.lastRecordedFailureAt).isAtLeast(before)
        assertThat(fake.lastRecordedFailureAt).isAtMost(after)
    }

    @Test
    fun `recordSuccess uses current time as default`() = runTest {
        val fake = FakeRepository()
        val repository: PlaybackCompatibilityRepository = fake
        val key = PlaybackCompatibilityKey(
            deviceFingerprint = "fp",
            deviceModel = "model",
            androidSdk = 30,
            streamType = "HLS",
            videoMimeType = "video/avc",
            resolutionBucket = "1080P",
            decoderName = "dec",
            surfaceType = "surf"
        )

        val before = System.currentTimeMillis()
        repository.recordSuccess(key)
        val after = System.currentTimeMillis()

        assertThat(fake.lastRecordedSuccessAt).isAtLeast(before)
        assertThat(fake.lastRecordedSuccessAt).isAtMost(after)
    }

    @Test
    fun `prune uses defaults for maxRecords and olderThanMs`() = runTest {
        val fake = FakeRepository()
        val repository: PlaybackCompatibilityRepository = fake

        val before = System.currentTimeMillis() - PlaybackCompatibilityRepository.DEFAULT_RETENTION_MS
        repository.prune()
        val after = System.currentTimeMillis() - PlaybackCompatibilityRepository.DEFAULT_RETENTION_MS

        assertThat(fake.lastPruneMaxRecords).isEqualTo(250)
        assertThat(fake.lastPruneOlderThanMs).isAtLeast(before)
        assertThat(fake.lastPruneOlderThanMs).isAtMost(after)
    }

    class FakeRepository : PlaybackCompatibilityRepository {
        var lastRecordedFailureAt: Long? = null
        var lastRecordedSuccessAt: Long? = null
        var lastPruneMaxRecords: Int? = null
        var lastPruneOlderThanMs: Long? = null

        override suspend fun getKnownBadRecords(
            deviceFingerprint: String,
            streamType: String,
            videoMimeType: String,
            resolutionBucket: String
        ): List<PlaybackCompatibilityRecord> = emptyList()

        override suspend fun recordFailure(key: PlaybackCompatibilityKey, failureType: String, at: Long) {
            lastRecordedFailureAt = at
        }

        override suspend fun recordSuccess(key: PlaybackCompatibilityKey, at: Long) {
            lastRecordedSuccessAt = at
        }

        override suspend fun prune(maxRecords: Int, olderThanMs: Long) {
            lastPruneMaxRecords = maxRecords
            lastPruneOlderThanMs = olderThanMs
        }
    }
}
