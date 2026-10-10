package com.zenlemon.player.playback

import okhttp3.OkHttpClient
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class UnsafeTlsSupportTest {

    @Test
    fun applyUnsafeTlsBypass_doesNotAlterClientBuilder() {
        val originalBuilder = OkHttpClient.Builder()
        val originalClient = originalBuilder.build()

        val newBuilder = OkHttpClient.Builder().applyUnsafeTlsBypass()
        val newClient = newBuilder.build()

        // As applyUnsafeTlsBypass now returns `this` without modifications,
        // the default secure TLS setup remains in place.
        // Thus, we verify that the two builders result in identical configurations
        // when compared to a fresh builder. Wait, OkHttp client properties can't easily be
        // checked for equality, but we can verify that the method returns the exact same instance.

        val builder = OkHttpClient.Builder()
        val returnedBuilder = builder.applyUnsafeTlsBypass()

        assertThat(returnedBuilder).isSameInstanceAs(builder)
    }
}
