package com.zenlemon.player

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LiveAudioTapTest {

    @Test
    fun `LiveAudioPcmBuffer properties are correctly assigned`() {
        val data = byteArrayOf(1, 2, 3)
        val presentationTimeUs = 123456L
        val sampleRate = 48000
        val channelCount = 2
        val encoding = 2 // ENCODING_PCM_16BIT

        val buffer = LiveAudioPcmBuffer(
            data = data,
            presentationTimeUs = presentationTimeUs,
            sampleRate = sampleRate,
            channelCount = channelCount,
            encoding = encoding
        )

        assertThat(buffer.data).isEqualTo(data)
        assertThat(buffer.presentationTimeUs).isEqualTo(presentationTimeUs)
        assertThat(buffer.sampleRate).isEqualTo(sampleRate)
        assertThat(buffer.channelCount).isEqualTo(channelCount)
        assertThat(buffer.encoding).isEqualTo(encoding)
    }

    @Test
    fun `LiveAudioTap can be implemented as lambda`() {
        var receivedBuffer: LiveAudioPcmBuffer? = null

        val tap = LiveAudioTap { buffer ->
            receivedBuffer = buffer
        }

        val testBuffer = LiveAudioPcmBuffer(
            data = byteArrayOf(),
            presentationTimeUs = 0L,
            sampleRate = 44100,
            channelCount = 1,
            encoding = 2
        )

        tap.onPcmAudio(testBuffer)

        assertThat(receivedBuffer).isSameInstanceAs(testBuffer)
    }
}
