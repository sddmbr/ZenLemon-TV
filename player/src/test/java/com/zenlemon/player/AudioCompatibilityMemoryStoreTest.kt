package com.zenlemon.player

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioCompatibilityMemoryStoreTest {

    private lateinit val context: Context
    private lateinit val store: AudioCompatibilityMemoryStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        store = AudioCompatibilityMemoryStore(context)
        store.clear()
    }

    @Test
    fun lookup_returnsNull_whenNotFound() {
        val result = store.lookup(mediaId = "test-media-1", streamType = "HLS")
        assertThat(result).isNull()
    }

    @Test
    fun rememberSoftwareAudioFallback_savesNormalizedData() {
        store.rememberSoftwareAudioFallback(
            mediaId = "test-media-2",
            streamType = "DASH",
            audioMimeTypes = listOf(" audio/mp4 ", "", "audio/aac", "audio/mp4"),
            detail = "Some|Details"
        )

        val result = store.lookup(mediaId = "test-media-2", streamType = "DASH")

        assertThat(result).isNotNull()
        assertThat(result?.mediaId).isEqualTo("test-media-2")
        assertThat(result?.streamType).isEqualTo("DASH")
        // Duplicates and empties removed, trimmed
        assertThat(result?.audioMimeTypes).containsExactly("audio/mp4", "audio/aac").inOrder()
        assertThat(result?.decision).isEqualTo(AudioCompatibilityMemoryStore.DECISION_SOFTWARE_FFMPEG)
        // Separator replaced with space
        assertThat(result?.detail).isEqualTo("Some Details")
        assertThat(result?.updatedAtMs).isGreaterThan(0L)
    }

    @Test
    fun lookup_returnsNull_whenDataIsMalformed() {
        // Manually write malformed data using the same key format
        val sharedPrefs = context.getSharedPreferences("audio_compatibility_memory", Context.MODE_PRIVATE)
        val key = "${Build.FINGERPRINT}|${Build.MODEL}|HLS|malformed-media"
        sharedPrefs.edit().putString(key, "audio/aac|software-ffmpeg").apply()

        val result = store.lookup(mediaId = "malformed-media", streamType = "HLS")

        assertThat(result).isNull()
    }

    @Test
    fun clear_removesAllEntries() {
        store.rememberSoftwareAudioFallback(
            mediaId = "test-media-3",
            streamType = "HLS",
            audioMimeTypes = listOf("audio/ac3"),
            detail = null
        )

        assertThat(store.lookup("test-media-3", "HLS")).isNotNull()

        store.clear()

        assertThat(store.lookup("test-media-3", "HLS")).isNull()
    }

    @Test
    fun lookup_handlesMissingDetailsGracefully() {
        store.rememberSoftwareAudioFallback(
            mediaId = "test-media-4",
            streamType = "HLS",
            audioMimeTypes = listOf("audio/eac3"),
            detail = null
        )

        val result = store.lookup("test-media-4", "HLS")

        assertThat(result).isNotNull()
        assertThat(result?.detail).isNull()
    }
}
