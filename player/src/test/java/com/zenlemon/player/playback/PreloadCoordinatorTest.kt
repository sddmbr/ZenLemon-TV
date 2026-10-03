package com.zenlemon.player.playback

import com.google.common.truth.Truth.assertThat
import com.zenlemon.domain.model.DrmInfo
import com.zenlemon.domain.model.DrmScheme
import com.zenlemon.domain.model.StreamInfo
import java.lang.reflect.Proxy
import androidx.media3.exoplayer.source.MediaSource
import org.junit.Test

class PreloadCoordinatorTest {

    @Test
    fun `same item same url under two minutes reuses`() {
        var now = 1_000L
        val coordinator = PreloadCoordinator { now }
        val source = fakeMediaSource()
        val info = StreamInfo(url = "http://example.com/movie.mp4")
        coordinator.store("1", info, ResolvedStreamType.PROGRESSIVE, source)

        assertThat(coordinator.tryReuse("1", info, ResolvedStreamType.PROGRESSIVE)).isSameInstanceAs(source)
    }

    @Test
    fun `tryReuse when empty returns null`() {
        val coordinator = PreloadCoordinator { 1_000L }
        val info = StreamInfo(url = "http://example.com/movie.mp4")
        assertThat(coordinator.tryReuse("1", info, ResolvedStreamType.PROGRESSIVE)).isNull()
    }

    @Test
    fun `tryReuse successfully clears source so it cannot be reused twice`() {
        var now = 1_000L
        val coordinator = PreloadCoordinator { now }
        val source = fakeMediaSource()
        val info = StreamInfo(url = "http://example.com/movie.mp4")
        coordinator.store("1", info, ResolvedStreamType.PROGRESSIVE, source)

        assertThat(coordinator.tryReuse("1", info, ResolvedStreamType.PROGRESSIVE)).isSameInstanceAs(source)
        assertThat(coordinator.tryReuse("1", info, ResolvedStreamType.PROGRESSIVE)).isNull()
    }

    @Test
    fun `different mediaId invalidates`() {
        var now = 1_000L
        val coordinator = PreloadCoordinator { now }
        val info = StreamInfo(url = "http://example.com/movie.mp4")
        coordinator.store("1", info, ResolvedStreamType.PROGRESSIVE, fakeMediaSource())

        assertThat(coordinator.tryReuse("2", info, ResolvedStreamType.PROGRESSIVE)).isNull()
    }

    @Test
    fun `different url invalidates`() {
        var now = 1_000L
        val coordinator = PreloadCoordinator { now }
        coordinator.store("1", StreamInfo(url = "http://example.com/a.mp4"), ResolvedStreamType.PROGRESSIVE, fakeMediaSource())

        assertThat(coordinator.tryReuse("1", StreamInfo(url = "http://example.com/b.mp4"), ResolvedStreamType.PROGRESSIVE)).isNull()
    }

    @Test
    fun `url is normalized so whitespace and fragments are ignored`() {
        var now = 1_000L
        val coordinator = PreloadCoordinator { now }
        val source = fakeMediaSource()
        coordinator.store("1", StreamInfo(url = " http://example.com/a.mp4 "), ResolvedStreamType.PROGRESSIVE, source)

        // Request with same URL but fragment added
        assertThat(coordinator.tryReuse("1", StreamInfo(url = "http://example.com/a.mp4#some-fragment"), ResolvedStreamType.PROGRESSIVE)).isSameInstanceAs(source)
    }

    @Test
    fun `different stream type invalidates`() {
        var now = 1_000L
        val coordinator = PreloadCoordinator { now }
        val info = StreamInfo(url = "http://example.com/live/channel.ts")
        coordinator.store("1", info, ResolvedStreamType.MPEG_TS_LIVE, fakeMediaSource())

        assertThat(coordinator.tryReuse("1", info, ResolvedStreamType.HLS)).isNull()
    }

    @Test
    fun `different drm info invalidates`() {
        var now = 1_000L
        val coordinator = PreloadCoordinator { now }
        val info1 = StreamInfo(
            url = "http://example.com/movie.mp4",
            drmInfo = DrmInfo(DrmScheme.WIDEVINE, "http://license")
        )
        val info2 = StreamInfo(
            url = "http://example.com/movie.mp4",
            drmInfo = DrmInfo(DrmScheme.WIDEVINE, "http://different-license")
        )
        coordinator.store("1", info1, ResolvedStreamType.PROGRESSIVE, fakeMediaSource())

        assertThat(coordinator.tryReuse("1", info2, ResolvedStreamType.PROGRESSIVE)).isNull()
    }

    @Test
    fun `different headers invalidates`() {
        var now = 1_000L
        val coordinator = PreloadCoordinator { now }
        val info1 = StreamInfo(
            url = "http://example.com/movie.mp4",
            headers = mapOf("Auth" to "token1")
        )
        val info2 = StreamInfo(
            url = "http://example.com/movie.mp4",
            headers = mapOf("Auth" to "token2")
        )
        coordinator.store("1", info1, ResolvedStreamType.PROGRESSIVE, fakeMediaSource())

        assertThat(coordinator.tryReuse("1", info2, ResolvedStreamType.PROGRESSIVE)).isNull()
    }

    @Test
    fun `different user agent invalidates`() {
        var now = 1_000L
        val coordinator = PreloadCoordinator { now }
        val info1 = StreamInfo(
            url = "http://example.com/movie.mp4",
            userAgent = "Agent1"
        )
        val info2 = StreamInfo(
            url = "http://example.com/movie.mp4",
            userAgent = "Agent2"
        )
        coordinator.store("1", info1, ResolvedStreamType.PROGRESSIVE, fakeMediaSource())

        assertThat(coordinator.tryReuse("1", info2, ResolvedStreamType.PROGRESSIVE)).isNull()
    }

    @Test
    fun `older than two minutes expires`() {
        var now = 1_000L
        val coordinator = PreloadCoordinator { now }
        val info = StreamInfo(
            url = "http://example.com/movie.mp4",
            drmInfo = DrmInfo(DrmScheme.WIDEVINE, "http://license")
        )
        coordinator.store("1", info, ResolvedStreamType.PROGRESSIVE, fakeMediaSource())
        now += 121_000L

        assertThat(coordinator.tryReuse("1", info, ResolvedStreamType.PROGRESSIVE)).isNull()
    }

    @Test
    fun `onPlaybackStarted with different mediaId invalidates`() {
        var now = 1_000L
        val coordinator = PreloadCoordinator { now }
        val info = StreamInfo(url = "http://example.com/movie.mp4")
        coordinator.store("1", info, ResolvedStreamType.PROGRESSIVE, fakeMediaSource())

        coordinator.onPlaybackStarted("2")

        assertThat(coordinator.tryReuse("1", info, ResolvedStreamType.PROGRESSIVE)).isNull()
    }

    @Test
    fun `onPlaybackStarted with same mediaId does not invalidate`() {
        var now = 1_000L
        val coordinator = PreloadCoordinator { now }
        val info = StreamInfo(url = "http://example.com/movie.mp4")
        val source = fakeMediaSource()
        coordinator.store("1", info, ResolvedStreamType.PROGRESSIVE, source)

        coordinator.onPlaybackStarted("1")

        assertThat(coordinator.tryReuse("1", info, ResolvedStreamType.PROGRESSIVE)).isSameInstanceAs(source)
    }

    @Test
    fun `release invalidates source`() {
        var now = 1_000L
        val coordinator = PreloadCoordinator { now }
        val info = StreamInfo(url = "http://example.com/movie.mp4")
        coordinator.store("1", info, ResolvedStreamType.PROGRESSIVE, fakeMediaSource())

        coordinator.release()

        assertThat(coordinator.tryReuse("1", info, ResolvedStreamType.PROGRESSIVE)).isNull()
    }

    private fun fakeMediaSource(): MediaSource {
        return Proxy.newProxyInstance(
            MediaSource::class.java.classLoader,
            arrayOf(MediaSource::class.java)
        ) { _, method, _ ->
            when (method.returnType) {
                Boolean::class.javaPrimitiveType -> false
                Int::class.javaPrimitiveType -> 0
                Long::class.javaPrimitiveType -> 0L
                Float::class.javaPrimitiveType -> 0f
                Double::class.javaPrimitiveType -> 0.0
                Unit::class.java -> Unit
                else -> null
            }
        } as MediaSource
    }
}
