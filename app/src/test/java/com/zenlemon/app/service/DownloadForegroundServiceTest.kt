package com.zenlemon.app.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.zenlemon.app.R
import com.zenlemon.domain.model.DownloadContentType
import com.zenlemon.domain.model.DownloadItem
import com.zenlemon.domain.model.DownloadStatus
import com.zenlemon.domain.repository.DownloadManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.shadows.ShadowNotificationManager
import org.robolectric.shadows.ShadowService

@RunWith(RobolectricTestRunner::class)
class DownloadForegroundServiceTest {

    private lateinit var serviceController: ServiceController<DownloadForegroundService>
    private lateinit var service: DownloadForegroundService
    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager
    private lateinit var shadowService: ShadowService
    private lateinit var mockedStaticEntryPoint: MockedStatic<EntryPointAccessors>

    private val downloadFlow = MutableStateFlow<DownloadItem?>(null)
    private val downloadManager = mock<DownloadManager> {
        on { observeDownload(any()) } doReturn downloadFlow
    }

    private val entryPoint = object : DownloadForegroundService.DownloadServiceEntryPoint {
        override fun downloadManager(): DownloadManager = downloadManager
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)

        mockedStaticEntryPoint = mockStatic(EntryPointAccessors::class.java)
        mockedStaticEntryPoint.`when`<Any> {
            EntryPointAccessors.fromApplication(any(), any<Class<*>>())
        }.thenReturn(entryPoint)

        serviceController = Robolectric.buildService(DownloadForegroundService::class.java)
        service = serviceController.create().get()
        shadowService = shadowOf(service)
    }

    @After
    fun teardown() {
        mockedStaticEntryPoint.close()
        serviceController.destroy()
    }

    @Test
    fun `onStartCommand with null downloadId stops self`() {
        val intent = Intent(context, DownloadForegroundService::class.java)

        serviceController.withIntent(intent).startCommand(0, 1)

        assertThat(shadowService.isStoppedBySelf).isTrue()
    }

    @Test
    fun `onStartCommand with empty downloadId stops self`() {
        val intent = Intent(context, DownloadForegroundService::class.java).apply {
            putExtra("download_id", "  ")
        }

        serviceController.withIntent(intent).startCommand(0, 1)

        assertThat(shadowService.isStoppedBySelf).isTrue()
    }

    @Test
    fun `onStartCommand with valid downloadId starts foreground`() = runTest {
        val downloadId = "test_download_1"
        val intent = Intent(context, DownloadForegroundService::class.java).apply {
            putExtra("download_id", downloadId)
        }

        serviceController.withIntent(intent).startCommand(0, 1)

        val notification = shadowService.lastForegroundNotification
        assertThat(notification).isNotNull()
        val title = notification.extras.getString(Notification.EXTRA_TITLE)
        assertThat(title).isEqualTo("Preparing download service")

        assertThat(shadowService.isStoppedBySelf).isFalse()
    }

    @Test
    fun `download updates reflect in notification`() = runTest {
        val downloadId = "test_download_1"
        val intent = Intent(context, DownloadForegroundService::class.java).apply {
            putExtra("download_id", downloadId)
        }

        serviceController.withIntent(intent).startCommand(0, 1)

        val downloadItem = DownloadItem(
            id = downloadId,
            providerId = 1L,
            contentType = DownloadContentType.MOVIE,
            contentId = 100L,
            contentName = "Test Movie",
            streamUrl = "http://test",
            status = DownloadStatus.DOWNLOADING,
            bytesWritten = 500L,
            totalBytes = 1024L
        )

        downloadFlow.value = downloadItem

        // Let coroutines execute
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        val notification = shadowNotificationManager.getNotification(2001) // NOTIFICATION_ID
        assertThat(notification).isNotNull()

        val title = notification.extras.getString(Notification.EXTRA_TITLE)
        assertThat(title).isEqualTo("Test Movie")

        val text = notification.extras.getString(Notification.EXTRA_TEXT)
        assertThat(text).contains("Test Movie")
        assertThat(text).contains("Downloading")

        assertThat(shadowService.isStoppedBySelf).isFalse()
    }

    @Test
    fun `completed download stops service`() = runTest {
        val downloadId = "test_download_1"
        val intent = Intent(context, DownloadForegroundService::class.java).apply {
            putExtra("download_id", downloadId)
        }

        serviceController.withIntent(intent).startCommand(0, 1)

        val downloadItem = DownloadItem(
            id = downloadId,
            providerId = 1L,
            contentType = DownloadContentType.MOVIE,
            contentId = 100L,
            contentName = "Test Movie",
            streamUrl = "http://test",
            status = DownloadStatus.COMPLETED,
            bytesWritten = 1024L,
            totalBytes = 1024L
        )

        downloadFlow.value = downloadItem
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        val notification = shadowNotificationManager.getNotification(2001)
        assertThat(notification).isNotNull()
        val text = notification.extras.getString(Notification.EXTRA_TEXT)
        assertThat(text).isEqualTo(context.getString(R.string.download_completed))

        assertThat(shadowService.isStoppedBySelf).isTrue()
    }

    @Test
    fun `failed download stops service`() = runTest {
        val downloadId = "test_download_1"
        val intent = Intent(context, DownloadForegroundService::class.java).apply {
            putExtra("download_id", downloadId)
        }

        serviceController.withIntent(intent).startCommand(0, 1)

        val downloadItem = DownloadItem(
            id = downloadId,
            providerId = 1L,
            contentType = DownloadContentType.MOVIE,
            contentId = 100L,
            contentName = "Test Movie",
            streamUrl = "http://test",
            status = DownloadStatus.FAILED,
            failureReason = "Network error",
            bytesWritten = 500L,
            totalBytes = 1024L
        )

        downloadFlow.value = downloadItem
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        val notification = shadowNotificationManager.getNotification(2001)
        assertThat(notification).isNotNull()
        val text = notification.extras.getString(Notification.EXTRA_TEXT)
        assertThat(text).isEqualTo(context.getString(R.string.download_failed, "Network error"))

        assertThat(shadowService.isStoppedBySelf).isTrue()
    }

    @Test
    fun `cancelled download stops service`() = runTest {
        val downloadId = "test_download_1"
        val intent = Intent(context, DownloadForegroundService::class.java).apply {
            putExtra("download_id", downloadId)
        }

        serviceController.withIntent(intent).startCommand(0, 1)

        val downloadItem = DownloadItem(
            id = downloadId,
            providerId = 1L,
            contentType = DownloadContentType.MOVIE,
            contentId = 100L,
            contentName = "Test Movie",
            streamUrl = "http://test",
            status = DownloadStatus.CANCELLED,
            bytesWritten = 500L,
            totalBytes = 1024L
        )

        downloadFlow.value = downloadItem
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        val notification = shadowNotificationManager.getNotification(2001)
        assertThat(notification).isNotNull()
        val text = notification.extras.getString(Notification.EXTRA_TEXT)
        assertThat(text).isEqualTo(context.getString(R.string.download_cancelled))

        assertThat(shadowService.isStoppedBySelf).isTrue()
    }

    @Test
    fun `download bytes formatting handles edge cases`() = runTest {
        val downloadId = "test_download_1"
        val intent = Intent(context, DownloadForegroundService::class.java).apply {
            putExtra("download_id", downloadId)
        }

        serviceController.withIntent(intent).startCommand(0, 1)

        // Bytes < 1024
        downloadFlow.value = DownloadItem(
            id = downloadId,
            providerId = 1L,
            contentType = DownloadContentType.MOVIE,
            contentId = 100L,
            contentName = "Test Movie",
            streamUrl = "http://test",
            status = DownloadStatus.DOWNLOADING,
            bytesWritten = 500L,
            totalBytes = null // totalBytes null fallback
        )
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        var notification = shadowNotificationManager.getNotification(2001)
        var text = notification.extras.getString(Notification.EXTRA_TEXT)
        assertThat(text).contains("500 B written")

        // Bytes < 1 MB
        downloadFlow.value = DownloadItem(
            id = downloadId,
            providerId = 1L,
            contentType = DownloadContentType.MOVIE,
            contentId = 100L,
            contentName = "Test Movie",
            streamUrl = "http://test",
            status = DownloadStatus.DOWNLOADING,
            bytesWritten = 2048L,
            totalBytes = 4096L
        )
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        notification = shadowNotificationManager.getNotification(2001)
        text = notification.extras.getString(Notification.EXTRA_TEXT)
        assertThat(text).contains("2 KB / 4 KB")

        // Bytes < 1 GB
        downloadFlow.value = DownloadItem(
            id = downloadId,
            providerId = 1L,
            contentType = DownloadContentType.MOVIE,
            contentId = 100L,
            contentName = "Test Movie",
            streamUrl = "http://test",
            status = DownloadStatus.DOWNLOADING,
            bytesWritten = 1048576L * 5, // 5 MB
            totalBytes = 1048576L * 10
        )
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        notification = shadowNotificationManager.getNotification(2001)
        text = notification.extras.getString(Notification.EXTRA_TEXT)
        assertThat(text).contains("5 MB / 10 MB")

        // Bytes > 1 GB
        downloadFlow.value = DownloadItem(
            id = downloadId,
            providerId = 1L,
            contentType = DownloadContentType.MOVIE,
            contentId = 100L,
            contentName = "Test Movie",
            streamUrl = "http://test",
            status = DownloadStatus.DOWNLOADING,
            bytesWritten = 1073741824L * 2, // 2 GB
            totalBytes = 1073741824L * 3
        )
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        notification = shadowNotificationManager.getNotification(2001)
        text = notification.extras.getString(Notification.EXTRA_TEXT)
        assertThat(text).contains("2 GB / 3 GB")
    }
}
