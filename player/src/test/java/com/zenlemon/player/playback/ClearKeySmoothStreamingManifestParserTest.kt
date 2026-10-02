package com.zenlemon.player.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.smoothstreaming.manifest.SsManifest
import androidx.media3.exoplayer.smoothstreaming.manifest.SsManifestParser
import androidx.media3.extractor.mp4.TrackEncryptionBox
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.ByteArrayInputStream

@RunWith(AndroidJUnit4::class)
class ClearKeySmoothStreamingManifestParserTest {

    private fun createStreamElement(type: Int): SsManifest.StreamElement {
        return SsManifest.StreamElement(
            "baseUri",
            "chunkTemplate",
            type,
            when (type) {
                C.TRACK_TYPE_VIDEO -> "video/mp4"
                C.TRACK_TYPE_AUDIO -> "audio/mp4"
                else -> "text/vtt"
            },
            1000L,
            "name",
            0,
            100,
            100,
            100, // displayHeight
            "en",
            arrayOf(Format.Builder().setId("${type}_format").build()),
            emptyList(),
            0L
        )
    }

    @Test
    fun `parse with no protection element returns original manifest`() {
        val streamElement = createStreamElement(C.TRACK_TYPE_VIDEO)
        val manifest = SsManifest(
            1, 1, 1000L, 1000L, 0L, 0, false, null, arrayOf(streamElement)
        )
        val mockParser = mock<SsManifestParser> {
            on { parse(any(), any()) } doReturn manifest
        }
        val parser = ClearKeySmoothStreamingManifestParser(mockParser)

        val result = parser.parse(Uri.parse("http://example.com"), ByteArrayInputStream(ByteArray(0)))

        assertThat(result.protectionElement).isNull()
        assertThat(result.streamElements).isEqualTo(manifest.streamElements)
    }

    @Test
    fun `parse with protection element but no valid key IDs returns original manifest`() {
        val streamElement = createStreamElement(C.TRACK_TYPE_VIDEO)
        // Invalid key size (not 16 bytes)
        val invalidKeyId = ByteArray(10) { 1 }
        val trackEncryptionBox = TrackEncryptionBox(
            true, "cenc", 8, invalidKeyId, 0, 0, null
        )
        val protectionElement = SsManifest.ProtectionElement(
            C.WIDEVINE_UUID, ByteArray(0), arrayOf(trackEncryptionBox)
        )
        val manifest = SsManifest(
            1, 1, 1000L, 1000L, 0L, 0, false, protectionElement, arrayOf(streamElement)
        )
        val mockParser = mock<SsManifestParser> {
            on { parse(any(), any()) } doReturn manifest
        }
        val parser = ClearKeySmoothStreamingManifestParser(mockParser)

        val result = parser.parse(Uri.parse("http://example.com"), ByteArrayInputStream(ByteArray(0)))

        assertThat(result.protectionElement).isEqualTo(protectionElement)
        assertThat(result.streamElements).isEqualTo(manifest.streamElements)
    }

    @Test
    fun `parse with valid key IDs patches protection and stream elements`() {
        val streamElement = createStreamElement(C.TRACK_TYPE_VIDEO)
        val audioStreamElement = createStreamElement(C.TRACK_TYPE_AUDIO)
        val textStreamElement = createStreamElement(C.TRACK_TYPE_TEXT)

        val validKeyId = ByteArray(16) { it.toByte() }
        val trackEncryptionBox = TrackEncryptionBox(
            true, "cenc", 8, validKeyId, 0, 0, null
        )
        val protectionElement = SsManifest.ProtectionElement(
            C.PLAYREADY_UUID, ByteArray(0), arrayOf(trackEncryptionBox)
        )
        val manifest = SsManifest(
            1, 1, 1_000_000L, 5000000L, 0L, 0, false, protectionElement,
            arrayOf(streamElement, audioStreamElement, textStreamElement)
        )
        val mockParser = mock<SsManifestParser> {
            on { parse(any(), any()) } doReturn manifest
        }
        val parser = ClearKeySmoothStreamingManifestParser(mockParser)

        val result = parser.parse(Uri.parse("http://example.com"), ByteArrayInputStream(ByteArray(0)))

        // Verify protection element was updated
        assertThat(result.protectionElement).isNotNull()
        assertThat(result.protectionElement!!.uuid).isEqualTo(C.CLEARKEY_UUID)
        assertThat(result.protectionElement!!.data).isNotEmpty()

        // Verify DRM init data was added to video and audio formats
        val videoFormat = result.streamElements[0].formats[0]
        assertThat(videoFormat.drmInitData).isNotNull()
        assertThat(videoFormat.drmInitData!!.get(0).uuid).isEqualTo(C.CLEARKEY_UUID)
        assertThat(videoFormat.drmInitData!!.get(0).mimeType).isEqualTo(MimeTypes.VIDEO_MP4)

        val audioFormat = result.streamElements[1].formats[0]
        assertThat(audioFormat.drmInitData).isNotNull()
        assertThat(audioFormat.drmInitData!!.get(0).uuid).isEqualTo(C.CLEARKEY_UUID)

        // Verify text stream element was not touched
        val textFormat = result.streamElements[2].formats[0]
        assertThat(textFormat.drmInitData).isNull()
    }
}
