package com.zenlemon.player.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlaybackBufferPolicyTest {

    @Test
    fun `default values are set correctly`() {
        val policy = PlaybackBufferPolicy(
            label = "test-label",
            minBufferMs = 1000,
            maxBufferMs = 2000,
            playbackBufferMs = 500,
            rebufferMs = 1500,
            targetBufferBytes = 1024,
            prioritizeTimeOverSizeThresholds = true
        )

        assertThat(policy.label).isEqualTo("test-label")
        assertThat(policy.minBufferMs).isEqualTo(1000)
        assertThat(policy.maxBufferMs).isEqualTo(2000)
        assertThat(policy.playbackBufferMs).isEqualTo(500)
        assertThat(policy.rebufferMs).isEqualTo(1500)
        assertThat(policy.targetBufferBytes).isEqualTo(1024)
        assertThat(policy.prioritizeTimeOverSizeThresholds).isTrue()
        assertThat(policy.qualityReason).isEqualTo("baseline") // default value
        assertThat(policy.lowMemoryCapped).isFalse() // default value
    }

    @Test
    fun `can override default values`() {
        val policy = PlaybackBufferPolicy(
            label = "test-label",
            minBufferMs = 1000,
            maxBufferMs = 2000,
            playbackBufferMs = 500,
            rebufferMs = 1500,
            targetBufferBytes = 1024,
            prioritizeTimeOverSizeThresholds = false,
            qualityReason = "override-reason",
            lowMemoryCapped = true
        )

        assertThat(policy.prioritizeTimeOverSizeThresholds).isFalse()
        assertThat(policy.qualityReason).isEqualTo("override-reason")
        assertThat(policy.lowMemoryCapped).isTrue()
    }

    @Test
    fun `equals returns true for identical objects`() {
        val policy1 = PlaybackBufferPolicy(
            label = "test-label",
            minBufferMs = 1000,
            maxBufferMs = 2000,
            playbackBufferMs = 500,
            rebufferMs = 1500,
            targetBufferBytes = 1024,
            prioritizeTimeOverSizeThresholds = true
        )

        val policy2 = PlaybackBufferPolicy(
            label = "test-label",
            minBufferMs = 1000,
            maxBufferMs = 2000,
            playbackBufferMs = 500,
            rebufferMs = 1500,
            targetBufferBytes = 1024,
            prioritizeTimeOverSizeThresholds = true
        )

        assertThat(policy1).isEqualTo(policy2)
    }

    @Test
    fun `equals returns false for different objects`() {
        val policy1 = PlaybackBufferPolicy(
            label = "test-label-1",
            minBufferMs = 1000,
            maxBufferMs = 2000,
            playbackBufferMs = 500,
            rebufferMs = 1500,
            targetBufferBytes = 1024,
            prioritizeTimeOverSizeThresholds = true
        )

        val policy2 = PlaybackBufferPolicy(
            label = "test-label-2",
            minBufferMs = 1000,
            maxBufferMs = 2000,
            playbackBufferMs = 500,
            rebufferMs = 1500,
            targetBufferBytes = 1024,
            prioritizeTimeOverSizeThresholds = true
        )

        assertThat(policy1).isNotEqualTo(policy2)
    }

    @Test
    fun `hashCode is same for identical objects`() {
        val policy1 = PlaybackBufferPolicy(
            label = "test-label",
            minBufferMs = 1000,
            maxBufferMs = 2000,
            playbackBufferMs = 500,
            rebufferMs = 1500,
            targetBufferBytes = 1024,
            prioritizeTimeOverSizeThresholds = true
        )

        val policy2 = PlaybackBufferPolicy(
            label = "test-label",
            minBufferMs = 1000,
            maxBufferMs = 2000,
            playbackBufferMs = 500,
            rebufferMs = 1500,
            targetBufferBytes = 1024,
            prioritizeTimeOverSizeThresholds = true
        )

        assertThat(policy1.hashCode()).isEqualTo(policy2.hashCode())
    }
}
