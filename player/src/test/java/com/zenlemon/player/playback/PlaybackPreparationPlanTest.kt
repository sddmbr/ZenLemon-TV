package com.zenlemon.player.playback

import com.google.common.truth.Truth.assertThat
import com.zenlemon.domain.model.StreamInfo
import org.junit.Test
import androidx.media3.exoplayer.source.BehindLiveWindowException

class PlaybackPreparationPlanTest {

    @Test
    fun `refreshed stream info recomputes transport contract`() {
        val initial = buildPlaybackPreparationPlan(
            streamInfo = StreamInfo(url = "https://example.com/live/channel.m3u8"),
            preload = false,
            playbackStarted = { true }
        )

        val refreshed = buildPlaybackPreparationPlan(
            streamInfo = StreamInfo(url = "https://example.com/live/channel.ts", containerExtension = "ts"),
            preload = false,
            playbackStarted = { true }
        )

        assertThat(initial.resolvedStreamType).isEqualTo(ResolvedStreamType.HLS)
        assertThat(initial.timeoutProfile).isEqualTo(PlayerTimeoutProfile.LIVE)
        assertThat(refreshed.resolvedStreamType).isEqualTo(ResolvedStreamType.MPEG_TS_LIVE)
        assertThat(refreshed.timeoutProfile).isEqualTo(PlayerTimeoutProfile.LIVE)
        assertThat(refreshed.retryContext).isEqualTo(
            PlaybackRetryContext(
                resolvedStreamType = ResolvedStreamType.MPEG_TS_LIVE,
                timeoutProfile = PlayerTimeoutProfile.LIVE
            )
        )
    }

    @Test
    fun `preload plan uses preload timeout profile`() {
        val preloadPlan = buildPlaybackPreparationPlan(
            streamInfo = StreamInfo(url = "https://example.com/movie.mp4"),
            preload = true,
            playbackStarted = { false }
        )

        assertThat(preloadPlan.resolvedStreamType).isEqualTo(ResolvedStreamType.PROGRESSIVE)
        assertThat(preloadPlan.timeoutProfile).isEqualTo(PlayerTimeoutProfile.PRELOAD)
    }

    @Test
    fun `vod stream produces vod profile and retry context`() {
        val plan = buildPlaybackPreparationPlan(
            streamInfo = StreamInfo(url = "https://example.com/movie.mkv"),
            preload = false,
            playbackStarted = { false }
        )

        assertThat(plan.resolvedStreamType).isEqualTo(ResolvedStreamType.PROGRESSIVE)
        assertThat(plan.timeoutProfile).isEqualTo(PlayerTimeoutProfile.PROGRESSIVE)
        assertThat(plan.retryContext).isEqualTo(
            PlaybackRetryContext(
                resolvedStreamType = ResolvedStreamType.PROGRESSIVE,
                timeoutProfile = PlayerTimeoutProfile.PROGRESSIVE
            )
        )
        assertThat(plan.retryPolicy).isNotNull()
    }

    @Test
    fun `dash stream produces vod profile when not live`() {
        val plan = buildPlaybackPreparationPlan(
            streamInfo = StreamInfo(url = "https://example.com/manifest.mpd"),
            preload = false,
            playbackStarted = { false }
        )

        assertThat(plan.resolvedStreamType).isEqualTo(ResolvedStreamType.DASH)
        assertThat(plan.timeoutProfile).isEqualTo(PlayerTimeoutProfile.VOD)
        assertThat(plan.retryContext).isEqualTo(
            PlaybackRetryContext(
                resolvedStreamType = ResolvedStreamType.DASH,
                timeoutProfile = PlayerTimeoutProfile.VOD
            )
        )
        assertThat(plan.retryPolicy).isNotNull()
    }

    @Test
    fun `fastRetryOnTransientFailures and playbackStarted lambdas are accessible via retry policy`() {
        var fastRetryCalled = false

        val livePlan = buildPlaybackPreparationPlan(
            streamInfo = StreamInfo(url = "https://example.com/live/channel.m3u8"),
            preload = false,
            fastRetryOnTransientFailures = {
                fastRetryCalled = true
                true
            },
            playbackStarted = { true }
        )

        // This triggers the internal check for fastRetryOnTransientFailures via isFastRetryEligible
        livePlan.retryPolicy.retryDelayMs(BehindLiveWindowException(), 1)

        assertThat(fastRetryCalled).isTrue()
    }

    @Test
    fun `retry context correctly determines if live`() {
        val hlsContext = PlaybackRetryContext(ResolvedStreamType.HLS, PlayerTimeoutProfile.LIVE)
        assertThat(hlsContext.isLive).isTrue()

        val dashContext = PlaybackRetryContext(ResolvedStreamType.DASH, PlayerTimeoutProfile.VOD)
        assertThat(dashContext.isLive).isFalse()

        val smoothContext = PlaybackRetryContext(ResolvedStreamType.SMOOTH_STREAMING, PlayerTimeoutProfile.LIVE)
        assertThat(smoothContext.isLive).isTrue()

        val tsLiveContext = PlaybackRetryContext(ResolvedStreamType.MPEG_TS_LIVE, PlayerTimeoutProfile.LIVE)
        assertThat(tsLiveContext.isLive).isTrue()
    }
}
