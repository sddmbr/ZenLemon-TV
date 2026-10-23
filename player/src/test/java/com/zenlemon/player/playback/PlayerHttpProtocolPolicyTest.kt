package com.zenlemon.player.playback

import com.google.common.truth.Truth.assertThat
import com.zenlemon.domain.model.VodHttpProtocolMode
import org.junit.Test

class PlayerHttpProtocolPolicyTest {

    @Test
    fun `progressive vod streams force http1 in compatibility mode`() {
        assertThat(
            PlayerHttpProtocolPolicy.forceHttp1(
                resolvedStreamType = ResolvedStreamType.PROGRESSIVE,
                vodHttpProtocolMode = VodHttpProtocolMode.COMPATIBILITY_HTTP1
            )
        )
            .isTrue()
    }

    @Test
    fun `progressive vod streams keep default protocol negotiation in auto mode`() {
        assertThat(
            PlayerHttpProtocolPolicy.forceHttp1(
                resolvedStreamType = ResolvedStreamType.PROGRESSIVE,
                vodHttpProtocolMode = VodHttpProtocolMode.AUTO
            )
        )
            .isFalse()
    }

    @Test
    fun `live and adaptive streams keep default protocol negotiation even in compatibility mode`() {
        assertThat(
            PlayerHttpProtocolPolicy.forceHttp1(
                resolvedStreamType = ResolvedStreamType.HLS,
                vodHttpProtocolMode = VodHttpProtocolMode.COMPATIBILITY_HTTP1
            )
        )
            .isFalse()
        assertThat(
            PlayerHttpProtocolPolicy.forceHttp1(
                resolvedStreamType = ResolvedStreamType.DASH,
                vodHttpProtocolMode = VodHttpProtocolMode.COMPATIBILITY_HTTP1
            )
        )
            .isFalse()
        assertThat(
            PlayerHttpProtocolPolicy.forceHttp1(
                resolvedStreamType = ResolvedStreamType.MPEG_TS_LIVE,
                vodHttpProtocolMode = VodHttpProtocolMode.COMPATIBILITY_HTTP1
            )
        )
            .isFalse()
    }

    @Test
    fun `url based forceHttp1 defaults to false`() {
        assertThat(
            PlayerHttpProtocolPolicy.forceHttp1(
                url = "http://example.com/stream.m3u8",
                resolvedStreamType = ResolvedStreamType.HLS
            )
        ).isFalse()

        assertThat(
            PlayerHttpProtocolPolicy.forceHttp1(
                url = "https://example.com/video.mp4",
                resolvedStreamType = ResolvedStreamType.PROGRESSIVE
            )
        ).isFalse()

        assertThat(
            PlayerHttpProtocolPolicy.forceHttp1(
                url = "http://example.com/manifest.mpd",
                resolvedStreamType = ResolvedStreamType.DASH
            )
        ).isFalse()
    }
}
