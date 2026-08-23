package com.zenlemon.player.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StableHashTest {

    @Test
    fun `returns consistent 16-character hex string for known input`() {
        val hash = stableHash("hello")

        // sha256("hello") = 2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824
        // The function takes the first 8 bytes, which is 16 hex chars
        assertThat(hash).isEqualTo("2cf24dba5fb0a30e")
        assertThat(hash).hasLength(16)
    }

    @Test
    fun `returns consistent result on consecutive calls with the same input`() {
        val firstCall = stableHash("zenlemon")
        val secondCall = stableHash("zenlemon")

        assertThat(firstCall).isEqualTo(secondCall)
    }

    @Test
    fun `returns different results for different inputs`() {
        val hash1 = stableHash("hello")
        val hash2 = stableHash("world")

        assertThat(hash1).isNotEqualTo(hash2)
    }

    @Test
    fun `handles empty string correctly`() {
        val hash = stableHash("")

        // sha256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        assertThat(hash).isEqualTo("e3b0c44298fc1c14")
        assertThat(hash).hasLength(16)
    }
}
