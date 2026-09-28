package com.zenlemon.player.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CodecPreferenceTest {
    @Test
    fun `ActiveDecoderPolicy contains expected values`() {
        assertThat(ActiveDecoderPolicy.values().map { it.name }).containsExactly(
            "AUTO",
            "HARDWARE_PREFERRED",
            "SOFTWARE_PREFERRED",
            "COMPATIBILITY"
        )
    }

    @Test
    fun `ActiveDecoderPolicy values can be parsed from string`() {
        assertThat(ActiveDecoderPolicy.valueOf("AUTO")).isEqualTo(ActiveDecoderPolicy.AUTO)
        assertThat(ActiveDecoderPolicy.valueOf("HARDWARE_PREFERRED")).isEqualTo(ActiveDecoderPolicy.HARDWARE_PREFERRED)
        assertThat(ActiveDecoderPolicy.valueOf("SOFTWARE_PREFERRED")).isEqualTo(ActiveDecoderPolicy.SOFTWARE_PREFERRED)
    }
}
