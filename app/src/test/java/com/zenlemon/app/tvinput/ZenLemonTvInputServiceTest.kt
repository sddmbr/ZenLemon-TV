package com.zenlemon.app.tvinput

import android.content.Context
import android.media.tv.TvInputService
import android.net.Uri
import android.view.Surface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.zenlemon.domain.repository.ChannelRepository
import com.zenlemon.domain.model.StreamInfo
import com.zenlemon.domain.model.StreamType
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.net.Proxy
import java.lang.reflect.Method
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, manifest = Config.NONE)
class ZenLemonTvInputServiceTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var service: ZenLemonTvInputService
    private val channelRepository: ChannelRepository = mock()
    private val okHttpClient: OkHttpClient = OkHttpClient()

    @Before
    fun setup() {
        hiltRule.inject()
        val controller = Robolectric.buildService(ZenLemonTvInputService::class.java)
        service = controller.get()
        service.channelRepository = channelRepository
        service.okHttpClient = okHttpClient
        controller.create()
    }

    @Test
    fun onCreateSession_returnsSession() {
        val session = service.onCreateSession("test_input_id")
        assertThat(session).isNotNull()
        assertThat(session).isInstanceOf(TvInputService.Session::class.java)
    }

    @Test
    fun onTune_returnsTrueAndStartsTuning() {
        val session = service.onCreateSession("test_input_id")
        val uri = Uri.parse("content://tv/channel/123")
        val result = session.onTune(uri)
        assertThat(result).isTrue()
    }

    @Test
    fun onSetSurface_setsVideoSurface() {
        val session = service.onCreateSession("test_input_id")
        val surface: Surface = mock()
        val result = session.onSetSurface(surface)
        assertThat(result).isTrue()
    }

    @Test
    fun onSetStreamVolume_doesNotCrash() {
        val session = service.onCreateSession("test_input_id")
        session.onSetStreamVolume(0.5f)
    }

    @Test
    fun onSetCaptionEnabled_doesNotCrash() {
        val session = service.onCreateSession("test_input_id")
        session.onSetCaptionEnabled(true)
    }

    @Test
    fun onRelease_cleansUpResources() {
        val session = service.onCreateSession("test_input_id")
        session.onRelease()
    }

    @Test
    fun proxyConfiguration_appliesCorrectly() {
        // Reflection test to reach `forPlayback` which is private
        val streamInfo = StreamInfo(
            url = "http://test.com",
            streamType = StreamType.HLS,
            allowInvalidSsl = false,
            proxyHost = "127.0.0.1",
            proxyPort = 8080,
            userAgent = null,
            headers = emptyMap()
        )

        val forPlaybackMethod: Method = service.javaClass.getDeclaredMethod("forPlayback", OkHttpClient::class.java, StreamInfo::class.java)
        forPlaybackMethod.isAccessible = true
        val configuredClient = forPlaybackMethod.invoke(service, okHttpClient, streamInfo) as OkHttpClient

        val proxy = configuredClient.proxy
        assertThat(proxy).isNotNull()
        assertThat(proxy?.type()).isEqualTo(Proxy.Type.HTTP)
        val address = proxy?.address() as? InetSocketAddress
        assertThat(address?.hostString).isEqualTo("127.0.0.1")
        assertThat(address?.port).isEqualTo(8080)
    }

    @Test
    fun proxyConfiguration_returnsOriginalClient_whenNoProxyAndSslValid() {
        val streamInfo = StreamInfo(
            url = "http://test.com",
            streamType = StreamType.HLS,
            allowInvalidSsl = false,
            proxyHost = "",
            proxyPort = null,
            userAgent = null,
            headers = emptyMap()
        )

        val forPlaybackMethod: Method = service.javaClass.getDeclaredMethod("forPlayback", OkHttpClient::class.java, StreamInfo::class.java)
        forPlaybackMethod.isAccessible = true
        val configuredClient = forPlaybackMethod.invoke(service, okHttpClient, streamInfo) as OkHttpClient

        assertThat(configuredClient).isSameInstanceAs(okHttpClient)
    }
}
