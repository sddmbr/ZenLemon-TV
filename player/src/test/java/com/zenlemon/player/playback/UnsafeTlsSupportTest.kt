package com.zenlemon.player.playback

import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import org.junit.Test
import javax.net.ssl.SSLSession
import org.mockito.kotlin.mock

class UnsafeTlsSupportTest {

    @Test
    fun `applyUnsafeTlsBypass configures client to accept all hostnames`() {
        val builder = OkHttpClient.Builder()
        builder.applyUnsafeTlsBypass()
        val client = builder.build()

        val mockSession = mock<SSLSession>()

        // The verifier should return true for any hostname
        assertThat(client.hostnameVerifier.verify("invalid.test.example.com", mockSession)).isTrue()
        assertThat(client.hostnameVerifier.verify("localhost", mockSession)).isTrue()
    }

    @Test
    fun `applyUnsafeTlsBypass configures custom sslSocketFactory`() {
        val builder = OkHttpClient.Builder()
        val defaultClient = builder.build()
        val defaultSocketFactory = defaultClient.sslSocketFactory

        builder.applyUnsafeTlsBypass()
        val modifiedClient = builder.build()

        // Ensure that a new SSLSocketFactory has been set
        assertThat(modifiedClient.sslSocketFactory).isNotNull()
        assertThat(modifiedClient.sslSocketFactory).isNotEqualTo(defaultSocketFactory)
    }
}
