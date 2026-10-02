package com.zenlemon.player

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlayerTrackTest {

    @Test
    fun `PlayerTrack properties are correctly assigned`() {
        val track = PlayerTrack(
            id = "track_1",
            name = "English",
            language = "en",
            type = TrackType.AUDIO,
            isSelected = true
        )

        assertThat(track.id).isEqualTo("track_1")
        assertThat(track.name).isEqualTo("English")
        assertThat(track.language).isEqualTo("en")
        assertThat(track.type).isEqualTo(TrackType.AUDIO)
        assertThat(track.isSelected).isTrue()
    }

    @Test
    fun `PlayerTrack handles null language`() {
        val track = PlayerTrack(
            id = "track_2",
            name = "Unknown Language",
            language = null,
            type = TrackType.TEXT,
            isSelected = false
        )

        assertThat(track.id).isEqualTo("track_2")
        assertThat(track.name).isEqualTo("Unknown Language")
        assertThat(track.language).isNull()
        assertThat(track.type).isEqualTo(TrackType.TEXT)
        assertThat(track.isSelected).isFalse()
    }

    @Test
    fun `PlayerTrack equality works as expected for data class`() {
        val track1 = PlayerTrack(
            id = "track_1",
            name = "English",
            language = "en",
            type = TrackType.AUDIO,
            isSelected = true
        )
        val track2 = PlayerTrack(
            id = "track_1",
            name = "English",
            language = "en",
            type = TrackType.AUDIO,
            isSelected = true
        )
        val track3 = PlayerTrack(
            id = "track_3",
            name = "French",
            language = "fr",
            type = TrackType.TEXT,
            isSelected = false
        )

        assertThat(track1).isEqualTo(track2)
        assertThat(track1.hashCode()).isEqualTo(track2.hashCode())

        assertThat(track1).isNotEqualTo(track3)
    }
}
