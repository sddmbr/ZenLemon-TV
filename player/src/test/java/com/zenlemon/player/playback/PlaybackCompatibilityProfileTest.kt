package com.zenlemon.player.playback

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class PlaybackCompatibilityProfileTest {

    private val profile = DefaultPlaybackCompatibilityProfile

    @Before
    fun setUp() {
        // Reset to non-emulator defaults before each test
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "samsung/...")
        ReflectionHelpers.setStaticField(Build::class.java, "HARDWARE", "exynos")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "SM-G998B")
    }

    @Test
    fun `shouldDisableDecoderReuseWorkaround returns false for standard physical device`() {
        assertThat(profile.shouldDisableDecoderReuseWorkaround()).isFalse()
    }

    @Test
    fun `shouldDisableDecoderReuseWorkaround returns true when fingerprint contains generic`() {
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "generic_x86_arm")
        assertThat(profile.shouldDisableDecoderReuseWorkaround()).isTrue()
    }

    @Test
    fun `shouldDisableDecoderReuseWorkaround returns true when fingerprint contains emulator`() {
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "google/sdk_gphone64_arm64/emulator64_arm64")
        assertThat(profile.shouldDisableDecoderReuseWorkaround()).isTrue()
    }

    @Test
    fun `shouldDisableDecoderReuseWorkaround returns true when hardware contains goldfish`() {
        ReflectionHelpers.setStaticField(Build::class.java, "HARDWARE", "goldfish")
        assertThat(profile.shouldDisableDecoderReuseWorkaround()).isTrue()
    }

    @Test
    fun `shouldDisableDecoderReuseWorkaround returns true when hardware contains ranchu`() {
        ReflectionHelpers.setStaticField(Build::class.java, "HARDWARE", "ranchu")
        assertThat(profile.shouldDisableDecoderReuseWorkaround()).isTrue()
    }

    @Test
    fun `shouldDisableDecoderReuseWorkaround returns true when model contains android sdk built for`() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "Android SDK built for x86")
        assertThat(profile.shouldDisableDecoderReuseWorkaround()).isTrue()
    }
}
