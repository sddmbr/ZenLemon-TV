package com.zenlemon.player

import android.content.Context
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.File

class PlaybackSupportSnapshotStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mock {
            on { filesDir } doReturn tempFolder.root
        }
    }

    @Test
    fun write_createsFileAndWritesContent() {
        val store = PlaybackSupportSnapshotStore(context)

        store.write("test report content")

        val expectedFile = File(tempFolder.root, "diagnostics/crash/latest-playback-support.txt")
        assertThat(expectedFile.exists()).isTrue()
        assertThat(expectedFile.readText()).isEqualTo("test report content")
    }

    @Test
    fun write_handlesExceptionsQuietly() {
        val expectedFile = File(tempFolder.root, "diagnostics/crash/latest-playback-support.txt")
        expectedFile.parentFile?.mkdirs()
        // Create a directory where the file is expected, so writeText will throw an exception
        expectedFile.mkdir()

        val store = PlaybackSupportSnapshotStore(context)

        // This should not throw an exception due to runCatching
        store.write("test report content")
    }
}
