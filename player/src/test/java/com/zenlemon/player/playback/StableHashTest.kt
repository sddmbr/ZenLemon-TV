package com.zenlemon.player.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StableHashTest {

    @Test
    fun stableHash_consistentOutputForSameInput() {
        val input = "https://example.com/video.mp4"
        val hash1 = stableHash(input)
        val hash2 = stableHash(input)

        assertThat(hash1).isEqualTo(hash2)
    }

    @Test
    fun stableHash_returns16HexChars() {
        val input = "test_string"
        val hash = stableHash(input)

        assertThat(hash).hasLength(16)
        assertThat(hash).matches("^[a-f0-9]{16}$")
    }

    @Test
    fun stableHash_differentInputsProduceDifferentOutputs() {
        val input1 = "url_1"
        val input2 = "url_2"

        val hash1 = stableHash(input1)
        val hash2 = stableHash(input2)

        assertThat(hash1).isNotEqualTo(hash2)
    }

    @Test
    fun stableHash_handlesEmptyString() {
        val hash = stableHash("")

        assertThat(hash).hasLength(16)
        // SHA-256 of empty string is e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        // First 8 bytes (16 hex chars): e3b0c44298fc1c14
        assertThat(hash).isEqualTo("e3b0c44298fc1c14")
    }

    @Test
    fun stableHash_handlesNonAsciiCharacters() {
        val input = "тест" // Cyrillic
        val hash1 = stableHash(input)
        val hash2 = stableHash(input)

        assertThat(hash1).isEqualTo(hash2)
        assertThat(hash1).hasLength(16)
    }
}
