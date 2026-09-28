package com.zenlemon.player.audio

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.times
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.O])
class PlayerAudioFocusControllerTest {

    private lateinit var context: Context
    private lateinit var audioManager: AudioManager

    private val applyVolume: (Float) -> Unit = mock()
    private val setPlayWhenReady: (Boolean) -> Unit = mock()
    private val onAudioFocusDenied: () -> Unit = mock()

    private lateinit var controller: PlayerAudioFocusController

    @Before
    fun setUp() {
        context = mock()
        audioManager = mock()
        whenever(context.getSystemService(Context.AUDIO_SERVICE)).thenReturn(audioManager)

        controller = PlayerAudioFocusController(
            context = context,
            applyVolume = applyVolume,
            setPlayWhenReady = setPlayWhenReady,
            onAudioFocusDenied = onAudioFocusDenied
        )
    }

    @Test
    fun `initial state has correct default values`() {
        assertThat(controller.isMuted.value).isFalse()
        assertThat(controller.bypassAudioFocus).isFalse()
    }

    @Test
    fun `requestAudioFocusIfNeeded when bypassAudioFocus is true dispatches volume and returns true without requesting focus`() {
        controller.bypassAudioFocus = true

        val granted = controller.requestAudioFocusIfNeeded()

        assertThat(granted).isTrue()
        verify(audioManager, never()).requestAudioFocus(any<AudioFocusRequest>())
        verify(applyVolume).invoke(1f)
    }

    @Test
    fun `requestAudioFocusIfNeeded requests focus and returns true when granted`() {
        whenever(audioManager.requestAudioFocus(any<AudioFocusRequest>())).thenReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)

        val granted = controller.requestAudioFocusIfNeeded()

        assertThat(granted).isTrue()
        verify(audioManager).requestAudioFocus(any<AudioFocusRequest>())
        verify(applyVolume).invoke(1f)
    }

    @Test
    fun `requestAudioFocusIfNeeded returns false when focus denied`() {
        whenever(audioManager.requestAudioFocus(any<AudioFocusRequest>())).thenReturn(AudioManager.AUDIOFOCUS_REQUEST_FAILED)

        val granted = controller.requestAudioFocusIfNeeded()

        assertThat(granted).isFalse()
        verify(audioManager).requestAudioFocus(any<AudioFocusRequest>())
    }

    @Test
    fun `audioFocusChangeListener AUDIOFOCUS_LOSS pauses playback`() {
        // Use reflection because onAudioFocusChangeListener property in AudioFocusRequest isn't easily accessible until API 26 SDK and Robolectric sometimes fails resolving it in unit tests due to missing shadows.
        val listenerField = PlayerAudioFocusController::class.java.getDeclaredField("audioFocusChangeListener")
        listenerField.isAccessible = true
        val listener = listenerField.get(controller) as AudioManager.OnAudioFocusChangeListener

        whenever(audioManager.requestAudioFocus(any<AudioFocusRequest>())).thenReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        controller.requestAudioFocusIfNeeded()

        clearInvocations(applyVolume)

        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)

        verify(setPlayWhenReady).invoke(false)
        verify(applyVolume).invoke(1f) // Should re-apply volume after loss, though maybe muted or unchanged
    }

    @Test
    fun `audioFocusChangeListener AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ducks volume`() {
        val listenerField = PlayerAudioFocusController::class.java.getDeclaredField("audioFocusChangeListener")
        listenerField.isAccessible = true
        val listener = listenerField.get(controller) as AudioManager.OnAudioFocusChangeListener

        whenever(audioManager.requestAudioFocus(any<AudioFocusRequest>())).thenReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        controller.requestAudioFocusIfNeeded()

        clearInvocations(applyVolume)

        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)

        verify(applyVolume).invoke(0.2f) // 1f * 0.2f ducking factor
    }

    @Test
    fun `setVolume coerces value to range and applies it`() {
        clearInvocations(applyVolume)

        controller.setVolume(0.5f)
        verify(applyVolume).invoke(0.5f)

        clearInvocations(applyVolume)
        controller.setVolume(1.5f) // Should coerce to 1f
        verify(applyVolume).invoke(1f)

        clearInvocations(applyVolume)
        controller.setVolume(-1f) // Should coerce to 0f
        verify(applyVolume).invoke(0f)
    }

    @Test
    fun `setMuted toggles mute state and applies 0 volume when muted`() {
        controller.setVolume(0.8f) // Default volume to 0.8
        clearInvocations(applyVolume)

        controller.setMuted(true)

        assertThat(controller.isMuted.value).isTrue()
        verify(applyVolume).invoke(0f)

        // Mock re-requesting focus on unmute
        whenever(audioManager.requestAudioFocus(any<AudioFocusRequest>())).thenReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)

        clearInvocations(applyVolume)
        controller.setMuted(false)

        assertThat(controller.isMuted.value).isFalse()
        // When unmuting without focus, it will re-request focus.
        // requestAudioFocusIfNeeded() calls dispatchVolume() AND setMuted() calls dispatchVolume() again.
        verify(applyVolume, times(2)).invoke(0.8f) // restores volume
    }

    @Test
    fun `release abandons audio focus`() {
        whenever(audioManager.requestAudioFocus(any<AudioFocusRequest>())).thenReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        controller.requestAudioFocusIfNeeded() // First we need focus to abandon it

        controller.release()

        verify(audioManager).abandonAudioFocusRequest(any<AudioFocusRequest>())
    }
}
