package com.zenlemon.player.playback

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class PlaybackCompatibilityProfileTest {

    private fun setBuildProperties(fingerprint: String, hardware: String, model: String) {
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", fingerprint)
        ReflectionHelpers.setStaticField(Build::class.java, "HARDWARE", hardware)
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", model)
    }

    @Test
    fun `disables workaround on generic fingerprint`() {
        setBuildProperties("generic", "unknown", "unknown")
        assertThat(DefaultPlaybackCompatibilityProfile.shouldDisableDecoderReuseWorkaround()).isTrue()
    }

    @Test
    fun `disables workaround on emulator fingerprint`() {
        setBuildProperties("emulator", "unknown", "unknown")
        assertThat(DefaultPlaybackCompatibilityProfile.shouldDisableDecoderReuseWorkaround()).isTrue()
    }

    @Test
    fun `disables workaround on goldfish hardware`() {
        setBuildProperties("unknown", "goldfish", "unknown")
        assertThat(DefaultPlaybackCompatibilityProfile.shouldDisableDecoderReuseWorkaround()).isTrue()
    }

    @Test
    fun `disables workaround on ranchu hardware`() {
        setBuildProperties("unknown", "ranchu", "unknown")
        assertThat(DefaultPlaybackCompatibilityProfile.shouldDisableDecoderReuseWorkaround()).isTrue()
    }

    @Test
    fun `disables workaround on specific model`() {
        setBuildProperties("unknown", "unknown", "Android SDK built for x86")
        assertThat(DefaultPlaybackCompatibilityProfile.shouldDisableDecoderReuseWorkaround()).isTrue()
    }

    @Test
    fun `disables workaround is case insensitive`() {
        setBuildProperties("GENERIC", "GOLDFISH", "ANDROID SDK BUILT FOR X86")
        assertThat(DefaultPlaybackCompatibilityProfile.shouldDisableDecoderReuseWorkaround()).isTrue()
    }

    @Test
    fun `keeps workaround on real device`() {
        setBuildProperties("xiaomi/...", "qcom", "Mi 10")
        assertThat(DefaultPlaybackCompatibilityProfile.shouldDisableDecoderReuseWorkaround()).isFalse()
    }
}
