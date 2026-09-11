package com.zenlemon.app.diagnostics

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CrashReportStoreTest {

    private lateinit var context: Context
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        // Reset CrashReportStore state via reflection
        resetCrashReportStoreState()

        // Clear files dir
        context.filesDir.deleteRecursively()
        context.filesDir.mkdirs()
    }

    @After
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(defaultHandler)
        resetCrashReportStoreState()
    }

    private fun resetCrashReportStoreState() {
        val installedField = CrashReportStore::class.java.getDeclaredField("installed")
        installedField.isAccessible = true
        (installedField.get(CrashReportStore) as AtomicBoolean).set(false)

        val writingCrashField = CrashReportStore::class.java.getDeclaredField("writingCrash")
        writingCrashField.isAccessible = true
        (writingCrashField.get(CrashReportStore) as AtomicBoolean).set(false)
    }

    @Test
    fun latestReportReturnsNullWhenNoReportExists() {
        val summary = CrashReportStore.latestReport(context)
        assertThat(summary).isNull()
    }

    @Test
    fun latestReportFileCreatesDirectory() {
        val crashDir = File(context.filesDir, "diagnostics/crash")
        assertThat(crashDir.exists()).isFalse()

        val file = CrashReportStore.latestReportFile(context)

        assertThat(file.name).isEqualTo("latest-crash.txt")
        assertThat(crashDir.exists()).isTrue()
    }

    @Test
    fun latestReportParsesValidReportFile() {
        val file = CrashReportStore.latestReportFile(context)
        file.writeText("""
            ZenLemon Crash Report
            ========================
            Timestamp: 2023-10-27T10:00:00Z
            App Version: 1.0.0 (1)
            Build Type: debug
            Package: com.zenlemon.app
            Device: Google Pixel 7
            Hardware: qcom
            Android SDK: 33
            Android Release: 13
            Thread: main
            Exception: java.lang.RuntimeException
            Message: Test exception

            Stacktrace:
            java.lang.RuntimeException: Test exception
                at com.zenlemon.app.MainActivity.onCreate(MainActivity.kt:20)
        """.trimIndent())

        val summary = CrashReportStore.latestReport(context)

        assertThat(summary).isNotNull()
        assertThat(summary?.timestamp).isEqualTo("2023-10-27T10:00:00Z")
        assertThat(summary?.exception).isEqualTo("java.lang.RuntimeException")
        assertThat(summary?.fileName).isEqualTo("latest-crash.txt")
        assertThat(summary?.content).contains("Test exception")
    }

    @Test
    fun deleteLatestReportRemovesFile() {
        val file = CrashReportStore.latestReportFile(context)
        file.writeText("test crash content")
        assertThat(file.exists()).isTrue()

        val result = CrashReportStore.deleteLatestReport(context)

        assertThat(result).isTrue()
        assertThat(file.exists()).isFalse()
    }

    @Test
    @Suppress("DEPRECATION")
    fun buildShareIntentCreatesCorrectIntent() {
        val uri = Uri.parse("content://com.zenlemon.app.fileprovider/crash/latest-crash.txt")

        val intent = CrashReportStore.buildShareIntent(uri)

        assertThat(intent.action).isEqualTo(Intent.ACTION_CHOOSER)

        // Extract the actual SEND intent from the chooser
        val sendIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertThat(sendIntent).isNotNull()
        assertThat(sendIntent?.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(sendIntent?.type).isEqualTo("text/plain")
        assertThat(sendIntent?.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)).isEqualTo(uri)
        assertThat(sendIntent?.flags?.and(Intent.FLAG_GRANT_READ_URI_PERMISSION)).isNotEqualTo(0)
    }

    @Test
    fun installWritesCrashReportAndSanitizes() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        // Use a custom handler to prevent System.exit() when there's no previous handler
        var originalException: Throwable? = null
        Thread.setDefaultUncaughtExceptionHandler { _, e -> originalException = e }

        CrashReportStore.install(app)

        // Trigger crash handler
        val exception = RuntimeException("User token=secret123 failed http://example.com/api")
        Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(Thread.currentThread(), exception)

        assertThat(originalException).isEqualTo(exception)

        val file = CrashReportStore.latestReportFile(context)
        assertThat(file.exists()).isTrue()

        val content = file.readText()

        // Check basic properties
        assertThat(content).contains("ZenLemon Crash Report")
        assertThat(content).contains("Exception: java.lang.RuntimeException")

        // Check sanitization
        assertThat(content).contains("Message: User token=<redacted> failed http://<redacted-url>")
        assertThat(content).doesNotContain("secret123")
        assertThat(content).doesNotContain("example.com")

        // Check playback support
        val supportFile = File(context.filesDir, "diagnostics/crash/latest-playback-support.txt")
        assertThat(supportFile.exists()).isFalse() // We didn't create one
    }
}
