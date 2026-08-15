
package com.zenlemon.player.playback

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
class PlaybackCompatibilityProfileTest {

    @Test
    fun `shouldDisableDecoderReuseWorkaround returns true for generic fingerprint`() {
        ShadowBuild.setFingerprint("generic_fingerprint")
        ShadowBuild.setHardware("some_hardware")
        ShadowBuild.setModel("some_model")
        assertThat(DefaultPlaybackCompatibilityProfile.shouldDisableDecoderReuseWorkaround()).isTrue()
    }

    @Test
    fun `shouldDisableDecoderReuseWorkaround returns true for emulator fingerprint`() {
        ShadowBuild.setFingerprint("some_emulator_fingerprint")
        ShadowBuild.setHardware("some_hardware")
        ShadowBuild.setModel("some_model")
        assertThat(DefaultPlaybackCompatibilityProfile.shouldDisableDecoderReuseWorkaround()).isTrue()
    }

    @Test
    fun `shouldDisableDecoderReuseWorkaround returns true for goldfish hardware`() {
        ShadowBuild.setFingerprint("normal_fingerprint")
        ShadowBuild.setHardware("goldfish_hardware")
        ShadowBuild.setModel("some_model")
        assertThat(DefaultPlaybackCompatibilityProfile.shouldDisableDecoderReuseWorkaround()).isTrue()
    }

    @Test
    fun `shouldDisableDecoderReuseWorkaround returns true for ranchu hardware`() {
        ShadowBuild.setFingerprint("normal_fingerprint")
        ShadowBuild.setHardware("ranchu_hardware")
        ShadowBuild.setModel("some_model")
        assertThat(DefaultPlaybackCompatibilityProfile.shouldDisableDecoderReuseWorkaround()).isTrue()
    }

    @Test
    fun `shouldDisableDecoderReuseWorkaround returns true for android sdk built for model`() {
        ShadowBuild.setFingerprint("normal_fingerprint")
        ShadowBuild.setHardware("some_hardware")
        ShadowBuild.setModel("android sdk built for x86")
        assertThat(DefaultPlaybackCompatibilityProfile.shouldDisableDecoderReuseWorkaround()).isTrue()
    }

    @Test
    fun `shouldDisableDecoderReuseWorkaround returns false for real device`() {
        ShadowBuild.setFingerprint("google/raven/raven:13/TP1A.220905.004/8927612:user/release-keys")
        ShadowBuild.setHardware("raven")
        ShadowBuild.setModel("Pixel 6 Pro")
        assertThat(DefaultPlaybackCompatibilityProfile.shouldDisableDecoderReuseWorkaround()).isFalse()
    }
}
