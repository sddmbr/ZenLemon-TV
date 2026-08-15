package com.zenlemon.player.playback

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import com.google.common.truth.Truth.assertThat
import com.zenlemon.player.LiveAudioPcmBuffer
import com.zenlemon.player.LiveAudioTap
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.nio.ByteBuffer

@UnstableApi
class LiveAudioTapAudioSinkTest {

    @Test
    fun `configure propagates to delegate and saves format properties`() {
        val delegate: AudioSink = mock()
        val sink = LiveAudioTapAudioSink(delegate) { null }

        val format = Format.Builder()
            .setSampleRate(44100)
            .setChannelCount(2)
            .setPcmEncoding(C.ENCODING_PCM_16BIT)
            .build()

        sink.configure(format, 1024, intArrayOf(1, 2))

        verify(delegate).configure(eq(format), eq(1024), eq(intArrayOf(1, 2)))
    }

    @Test
    fun `handleBuffer delegates and skips tap if tapProvider returns null`() {
        val delegate: AudioSink = mock()
        val tapProvider: () -> LiveAudioTap? = { null }
        val sink = LiveAudioTapAudioSink(delegate, tapProvider)

        val format = Format.Builder()
            .setSampleRate(44100)
            .setChannelCount(2)
            .setPcmEncoding(C.ENCODING_PCM_16BIT)
            .build()
        sink.configure(format, 0, null)

        val buffer = ByteBuffer.allocate(10)
        buffer.put(ByteArray(10))
        buffer.flip()

        whenever(delegate.handleBuffer(any(), any(), any())).thenAnswer {
            val buf = it.arguments[0] as ByteBuffer
            buf.position(buf.limit()) // simulate full consume
            true
        }

        val result = sink.handleBuffer(buffer, 1000L, 1)
        assertThat(result).isTrue()

        // No exceptions and handled properly
    }

    @Test
    fun `handleBuffer delegates and skips tap if not 16-bit PCM`() {
        val delegate: AudioSink = mock()
        val tap: LiveAudioTap = mock()
        val sink = LiveAudioTapAudioSink(delegate) { tap }

        val format = Format.Builder()
            .setSampleRate(44100)
            .setChannelCount(2)
            .setPcmEncoding(C.ENCODING_PCM_8BIT) // Not 16-bit
            .build()
        sink.configure(format, 0, null)

        val buffer = ByteBuffer.allocate(10)
        buffer.put(ByteArray(10))
        buffer.flip()

        whenever(delegate.handleBuffer(any(), any(), any())).thenAnswer {
            val buf = it.arguments[0] as ByteBuffer
            buf.position(buf.limit())
            true
        }

        val result = sink.handleBuffer(buffer, 1000L, 1)
        assertThat(result).isTrue()

        verifyNoInteractions(tap)
    }

    @Test
    fun `handleBuffer delegates and skips tap if no bytes consumed`() {
        val delegate: AudioSink = mock()
        val tap: LiveAudioTap = mock()
        val sink = LiveAudioTapAudioSink(delegate) { tap }

        val format = Format.Builder()
            .setSampleRate(44100)
            .setChannelCount(2)
            .setPcmEncoding(C.ENCODING_PCM_16BIT)
            .build()
        sink.configure(format, 0, null)

        val buffer = ByteBuffer.allocate(10)
        buffer.put(ByteArray(10))
        buffer.flip()

        whenever(delegate.handleBuffer(any(), any(), any())).thenAnswer {
            // Buffer position not advanced
            true
        }

        val result = sink.handleBuffer(buffer, 1000L, 1)
        assertThat(result).isTrue()

        verifyNoInteractions(tap)
    }

    @Test
    fun `handleBuffer invokes tap with correct data and adjusted presentation time`() {
        val delegate: AudioSink = mock()
        val tap: LiveAudioTap = mock()
        val sink = LiveAudioTapAudioSink(delegate) { tap }

        val format = Format.Builder()
            .setSampleRate(44100)
            .setChannelCount(2)
            .setPcmEncoding(C.ENCODING_PCM_16BIT)
            .build()
        sink.configure(format, 0, null)

        val buffer = ByteBuffer.allocate(16) // 4 frames of 2 channels * 2 bytes
        val testData = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        buffer.put(testData)
        buffer.position(4) // Start position
        buffer.limit(12) // Limits to 8 bytes

        whenever(delegate.handleBuffer(any(), any(), any())).thenAnswer {
            val buf = it.arguments[0] as ByteBuffer
            buf.position(buf.position() + 8) // Consumes 8 bytes
            true
        }

        val initialPresentationTime = 10_000_000L // 10s

        val result = sink.handleBuffer(buffer, initialPresentationTime, 1)
        assertThat(result).isTrue()

        val captor = argumentCaptor<LiveAudioPcmBuffer>()
        verify(tap).onPcmAudio(captor.capture())

        val tapBuffer = captor.firstValue
        assertThat(tapBuffer.data).isEqualTo(testData.copyOfRange(4, 12))
        assertThat(tapBuffer.sampleRate).isEqualTo(44100)
        assertThat(tapBuffer.channelCount).isEqualTo(2)
        assertThat(tapBuffer.encoding).isEqualTo(C.ENCODING_PCM_16BIT)

        // bytePosition = 4
        // frameSize = 2 * 2 = 4
        // frames = 4 / 4 = 1
        // offsetUs = 1 * 1_000_000 / 44100 = 22
        assertThat(tapBuffer.presentationTimeUs).isEqualTo(10_000_022L)
    }
}
