package com.zenlemon.player.playback

import com.google.common.truth.Truth.assertThat
import com.zenlemon.domain.model.StreamInfo
import com.zenlemon.domain.model.StreamType
import org.junit.Test

class PlayerTimeoutProfileTest {

    @Test
    fun `hls live selects live timeout profile`() {
        assertThat(
            PlayerTimeoutProfile.resolve(
                streamInfo = StreamInfo(url = "http://example.com/live/1.m3u8"),
                resolvedStreamType = ResolvedStreamType.HLS,
                preload = false
            )
        ).isEqualTo(PlayerTimeoutProfile.LIVE)
    }

    @Test
    fun `preload always selects preload profile`() {
        assertThat(
            PlayerTimeoutProfile.resolve(
                streamInfo = StreamInfo(url = "http://example.com/movie.mp4"),
                resolvedStreamType = ResolvedStreamType.PROGRESSIVE,
                preload = true
            )
        ).isEqualTo(PlayerTimeoutProfile.PRELOAD)
    }

    @Test
    fun `progressive file selects progressive timeout profile`() {
        val profile =
            PlayerTimeoutProfile.resolve(
                streamInfo = StreamInfo(url = "http://example.com/movie.mp4"),
                resolvedStreamType = ResolvedStreamType.PROGRESSIVE,
                preload = false
            )

        assertThat(profile).isEqualTo(PlayerTimeoutProfile.PROGRESSIVE)
        assertThat(profile.connectTimeoutMs).isAtMost(5_000L)
        assertThat(profile.readTimeoutMs).isEqualTo(10_000L)
    }

    @Test
    fun `stream type rtsp selects live timeout profile`() {
        assertThat(
            PlayerTimeoutProfile.resolve(
                streamInfo = StreamInfo(url = "rtsp://example.com/stream", streamType = StreamType.RTSP),
                resolvedStreamType = ResolvedStreamType.UNKNOWN,
                preload = false
            )
        ).isEqualTo(PlayerTimeoutProfile.LIVE)
    }

    @Test
    fun `resolved stream type smooth streaming selects live timeout profile`() {
        assertThat(
            PlayerTimeoutProfile.resolve(
                streamInfo = StreamInfo(url = "http://example.com/stream.ism"),
                resolvedStreamType = ResolvedStreamType.SMOOTH_STREAMING,
                preload = false
            )
        ).isEqualTo(PlayerTimeoutProfile.LIVE)
    }

    @Test
    fun `resolved stream type mpeg ts live selects live timeout profile`() {
        assertThat(
            PlayerTimeoutProfile.resolve(
                streamInfo = StreamInfo(url = "http://example.com/live.ts"),
                resolvedStreamType = ResolvedStreamType.MPEG_TS_LIVE,
                preload = false
            )
        ).isEqualTo(PlayerTimeoutProfile.LIVE)
    }

    @Test
    fun `resolved stream type rtsp selects live timeout profile`() {
        assertThat(
            PlayerTimeoutProfile.resolve(
                streamInfo = StreamInfo(url = "rtsp://example.com/stream"),
                resolvedStreamType = ResolvedStreamType.RTSP,
                preload = false
            )
        ).isEqualTo(PlayerTimeoutProfile.LIVE)
    }

    @Test
    fun `stream type progressive selects progressive timeout profile`() {
        assertThat(
            PlayerTimeoutProfile.resolve(
                streamInfo = StreamInfo(url = "http://example.com/movie.mp4", streamType = StreamType.PROGRESSIVE),
                resolvedStreamType = ResolvedStreamType.UNKNOWN,
                preload = false
            )
        ).isEqualTo(PlayerTimeoutProfile.PROGRESSIVE)
    }

    @Test
    fun `fallback to vod timeout profile`() {
        val profile = PlayerTimeoutProfile.resolve(
            streamInfo = StreamInfo(url = "http://example.com/unknown"),
            resolvedStreamType = ResolvedStreamType.UNKNOWN,
            preload = false
        )

        assertThat(profile).isEqualTo(PlayerTimeoutProfile.VOD)
        assertThat(profile.connectTimeoutMs).isEqualTo(15_000L)
        assertThat(profile.readTimeoutMs).isEqualTo(45_000L)
        assertThat(profile.writeTimeoutMs).isEqualTo(30_000L)
    }
}
