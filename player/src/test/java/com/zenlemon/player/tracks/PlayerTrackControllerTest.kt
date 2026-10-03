package com.zenlemon.player.tracks

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.argumentCaptor
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetworkCapabilities

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28]) // API 28 has getActiveNetwork
class PlayerTrackControllerTest {

    private lateinit var context: Context

    private lateinit var mockContext: Context
    private lateinit var mockConnectivityManager: ConnectivityManager
    private lateinit var mockNetwork: Network
    private lateinit var mockCapabilities: NetworkCapabilities

    private lateinit var controller: PlayerTrackController
    private lateinit var mockController: PlayerTrackController

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        controller = PlayerTrackController(context)

        mockContext = mock()
        mockConnectivityManager = mock()
        mockNetwork = mock()
        mockCapabilities = mock()
        whenever(mockContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(mockConnectivityManager)
        whenever(mockConnectivityManager.activeNetwork).thenReturn(mockNetwork)
        whenever(mockConnectivityManager.getNetworkCapabilities(mockNetwork)).thenReturn(mockCapabilities)
        mockController = PlayerTrackController(mockContext)
    }

    @Test
    fun `resetSelections clears tracks and resets video selection`() {
        controller.resetSelections()

        assertThat(controller.availableAudioTracks.value).isEmpty()
        assertThat(controller.availableSubtitleTracks.value).isEmpty()
        assertThat(controller.availableVideoTracks.value).isEmpty()
    }

    @Test
    fun `setPreferredAudioLanguage sets preferred language correctly`() {
        val player = mock<ExoPlayer>()
        val initialParams = TrackSelectionParameters.Builder(context).build()
        whenever(player.trackSelectionParameters).thenReturn(initialParams)

        controller.setPreferredAudioLanguage(player, "es")

        val captor = argumentCaptor<TrackSelectionParameters>()
        verify(player).trackSelectionParameters = captor.capture()
        assertThat(captor.firstValue.preferredAudioLanguages).contains("es")
    }

    @Test
    fun `applyInitialParameters sets wifi max height constraints`() {
        whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true)
        whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)).thenReturn(false)
        mockController.setNetworkQualityPreferences(wifiMaxHeight = 720, ethernetMaxHeight = 1080)

        val player = mock<ExoPlayer>()
        whenever(player.trackSelectionParameters).thenReturn(TrackSelectionParameters.Builder(mockContext).build())

        mockController.applyInitialParameters(player, constrainResolutionForMultiView = false)

        val captor = argumentCaptor<TrackSelectionParameters>()
        verify(player).trackSelectionParameters = captor.capture()
        assertThat(captor.firstValue.maxVideoHeight).isEqualTo(720)
    }

    @Test
    fun `applyInitialParameters sets ethernet max height constraints`() {
        whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(false)
        whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)).thenReturn(true)
        mockController.setNetworkQualityPreferences(wifiMaxHeight = 720, ethernetMaxHeight = 1080)

        val player = mock<ExoPlayer>()
        whenever(player.trackSelectionParameters).thenReturn(TrackSelectionParameters.Builder(mockContext).build())

        mockController.applyInitialParameters(player, constrainResolutionForMultiView = false)

        val captor = argumentCaptor<TrackSelectionParameters>()
        verify(player).trackSelectionParameters = captor.capture()
        assertThat(captor.firstValue.maxVideoHeight).isEqualTo(1080)
    }

    @Test
    fun `applyInitialParameters constrains resolution for multiview`() {
        whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true)
        mockController.setNetworkQualityPreferences(wifiMaxHeight = 1080, ethernetMaxHeight = 1080)

        val player = mock<ExoPlayer>()
        whenever(player.trackSelectionParameters).thenReturn(TrackSelectionParameters.Builder(mockContext).build())

        mockController.applyInitialParameters(player, constrainResolutionForMultiView = true)

        val captor = argumentCaptor<TrackSelectionParameters>()
        verify(player).trackSelectionParameters = captor.capture()
        // multiview should constrain it to minOf(720, preference) -> 720
        assertThat(captor.firstValue.maxVideoHeight).isEqualTo(720)
    }

    @Test
    fun `selectVideoTrack sets auto track correctly`() {
        val player = mock<ExoPlayer>()
        whenever(player.trackSelectionParameters).thenReturn(TrackSelectionParameters.Builder(context).build())

        controller.selectVideoTrack(player, com.zenlemon.player.PLAYER_TRACK_AUTO_ID)

        val captor = argumentCaptor<TrackSelectionParameters>()
        verify(player).trackSelectionParameters = captor.capture()
        assertThat(captor.firstValue.overrides).isEmpty() // Auto clears overrides
    }

    @Test
    fun `selectSubtitleTrack clears overrides and disables text track when trackId is null`() {
        val player = mock<ExoPlayer>()
        whenever(player.trackSelectionParameters).thenReturn(TrackSelectionParameters.Builder(context).build())

        controller.selectSubtitleTrack(player, null)

        val captor = argumentCaptor<TrackSelectionParameters>()
        verify(player).trackSelectionParameters = captor.capture()
        assertThat(captor.firstValue.disabledTrackTypes).contains(androidx.media3.common.C.TRACK_TYPE_TEXT)
    }
}
