package com.zenlemon.player

import androidx.media3.ui.CaptionStyleCompat
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlayerSubtitleStyleTest {

    @Test
    fun `test PlayerSubtitleStyle properties are correctly assigned`() {
        val style = PlayerSubtitleStyle(
            textScale = 1.5f,
            foregroundColorArgb = 0xFFFF0000.toInt(),
            backgroundColorArgb = 0xFF00FF00.toInt(),
            edgeType = CaptionStyleCompat.EDGE_TYPE_NONE,
            bottomPaddingFraction = 0.1f,
            useEmbeddedStyles = false
        )

        assertThat(style.textScale).isEqualTo(1.5f)
        assertThat(style.foregroundColorArgb).isEqualTo(0xFFFF0000.toInt())
        assertThat(style.backgroundColorArgb).isEqualTo(0xFF00FF00.toInt())
        assertThat(style.edgeType).isEqualTo(CaptionStyleCompat.EDGE_TYPE_NONE)
        assertThat(style.bottomPaddingFraction).isEqualTo(0.1f)
        assertThat(style.useEmbeddedStyles).isFalse()
    }

    @Test
    fun `test PlayerSubtitleStyle default values`() {
        val style = PlayerSubtitleStyle()

        assertThat(style.textScale).isEqualTo(1f)
        assertThat(style.foregroundColorArgb).isEqualTo(0xFFFFFFFF.toInt())
        assertThat(style.backgroundColorArgb).isEqualTo(0x80000000.toInt())
        assertThat(style.edgeType).isEqualTo(CaptionStyleCompat.EDGE_TYPE_OUTLINE)
        assertThat(style.bottomPaddingFraction).isEqualTo(0.08f)
        assertThat(style.useEmbeddedStyles).isTrue()
    }

    @Test
    fun `test PlayerSubtitleStyle equality and hashCode`() {
        val style1 = PlayerSubtitleStyle(
            textScale = 1.5f,
            foregroundColorArgb = 0xFFFF0000.toInt(),
            backgroundColorArgb = 0xFF00FF00.toInt(),
            edgeType = CaptionStyleCompat.EDGE_TYPE_NONE,
            bottomPaddingFraction = 0.1f,
            useEmbeddedStyles = false
        )

        val style2 = PlayerSubtitleStyle(
            textScale = 1.5f,
            foregroundColorArgb = 0xFFFF0000.toInt(),
            backgroundColorArgb = 0xFF00FF00.toInt(),
            edgeType = CaptionStyleCompat.EDGE_TYPE_NONE,
            bottomPaddingFraction = 0.1f,
            useEmbeddedStyles = false
        )

        val style3 = PlayerSubtitleStyle()

        assertThat(style1).isEqualTo(style2)
        assertThat(style1.hashCode()).isEqualTo(style2.hashCode())

        assertThat(style1).isNotEqualTo(style3)
        assertThat(style1.hashCode()).isNotEqualTo(style3.hashCode())
    }
}
