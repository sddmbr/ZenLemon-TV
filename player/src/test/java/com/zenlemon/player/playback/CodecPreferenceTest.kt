package com.zenlemon.player.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CodecPreferenceTest {

    @Test
    fun `enum contains expected values`() {
        assertThat(CodecPreference.values().map { it.name }).containsExactly(
            "DEFAULT",
            "SOFTWARE_ONLY",
            "HARDWARE_ONLY"
        )
    }

    @Test
    fun `enum values can be parsed from string`() {
        assertThat(CodecPreference.valueOf("DEFAULT")).isEqualTo(CodecPreference.DEFAULT)
        assertThat(CodecPreference.valueOf("SOFTWARE_ONLY")).isEqualTo(CodecPreference.SOFTWARE_ONLY)
        assertThat(CodecPreference.valueOf("HARDWARE_ONLY")).isEqualTo(CodecPreference.HARDWARE_ONLY)
    }
}
