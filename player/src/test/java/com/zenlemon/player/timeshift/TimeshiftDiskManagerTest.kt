package com.zenlemon.player.timeshift

import android.content.Context
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class TimeshiftDiskManagerTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var cacheDir: File
    private lateinit var timeshiftDir: File

    @Before
    fun setUp() {
        // Create a temporary directory securely
        cacheDir = File.createTempFile("cacheDir", null).apply {
            delete()
            mkdirs()
        }
        timeshiftDir = File(cacheDir, "timeshift")
        timeshiftDir.mkdirs()

        `when`(mockContext.cacheDir).thenReturn(cacheDir)
    }

    @After
    fun tearDown() {
        cacheDir.deleteRecursively()
    }

    @Test
    fun cleanupStaleDirectories_deletesAllButActiveSessionDir() {
        val manager = TimeshiftDiskManager(mockContext)

        val activeDir = File(timeshiftDir, "activeSession").apply { mkdirs() }
        val staleDir1 = File(timeshiftDir, "staleSession1").apply { mkdirs() }
        val staleDir2 = File(timeshiftDir, "staleSession2").apply { mkdirs() }
        val randomFile = File(timeshiftDir, "randomFile.txt").apply { createNewFile() }

        manager.cleanupStaleDirectories(activeDir)

        assertThat(activeDir.exists()).isTrue()
        assertThat(staleDir1.exists()).isFalse()
        assertThat(staleDir2.exists()).isFalse()
        assertThat(randomFile.exists()).isFalse()
    }

    @Test
    fun cleanupStaleDirectories_withNullActiveSession_wipesEverything() {
        val manager = TimeshiftDiskManager(mockContext)

        val staleDir1 = File(timeshiftDir, "staleSession1").apply { mkdirs() }
        val staleDir2 = File(timeshiftDir, "staleSession2").apply { mkdirs() }

        manager.cleanupStaleDirectories(null)

        assertThat(staleDir1.exists()).isFalse()
        assertThat(staleDir2.exists()).isFalse()
        // timeshiftDir itself is not deleted, only its contents
        assertThat(timeshiftDir.exists()).isTrue()
    }

    @Test
    fun currentUsageBytes_calculatesTotalFileSize() {
        val manager = TimeshiftDiskManager(mockContext)

        val dir1 = File(timeshiftDir, "session1").apply { mkdirs() }
        File(dir1, "file1.txt").apply { writeText("Hello") } // 5 bytes
        File(dir1, "file2.txt").apply { writeText("World!") } // 6 bytes

        val dir2 = File(timeshiftDir, "session2").apply { mkdirs() }
        File(dir2, "file3.txt").apply { writeText("123") } // 3 bytes

        val totalExpected = 5L + 6L + 3L

        assertThat(manager.currentUsageBytes()).isEqualTo(totalExpected)
    }

    @Test
    fun isWithinBudget_returnsTrueWhenUnderBudget() {
        val manager = TimeshiftDiskManager(mockContext, maxBudgetBytes = 10L)

        val dir1 = File(timeshiftDir, "session1").apply { mkdirs() }
        File(dir1, "file1.txt").apply { writeText("12345") } // 5 bytes

        assertThat(manager.isWithinBudget()).isTrue()
    }

    @Test
    fun isWithinBudget_returnsFalseWhenOverBudget() {
        val manager = TimeshiftDiskManager(mockContext, maxBudgetBytes = 10L)

        val dir1 = File(timeshiftDir, "session1").apply { mkdirs() }
        File(dir1, "file1.txt").apply { writeText("12345678901") } // 11 bytes

        assertThat(manager.isWithinBudget()).isFalse()
    }

    @Test
    fun evictLruUntilWithinBudget_evictsOldestFirst() {
        val manager = TimeshiftDiskManager(mockContext, maxBudgetBytes = 20L) // Target is 16L (80%)

        val activeSession = File(timeshiftDir, "activeSession").apply { mkdirs() }
        File(activeSession, "active.txt").apply { writeText("1234") } // 4 bytes

        val oldestDir = File(timeshiftDir, "oldestSession").apply { mkdirs() }
        File(oldestDir, "oldest.txt").apply { writeText("12345678") } // 8 bytes
        oldestDir.setLastModified(1000)

        val middleDir = File(timeshiftDir, "middleSession").apply { mkdirs() }
        File(middleDir, "middle.txt").apply { writeText("123456") } // 6 bytes
        middleDir.setLastModified(2000)

        val newestDir = File(timeshiftDir, "newestSession").apply { mkdirs() }
        File(newestDir, "newest.txt").apply { writeText("12") } // 2 bytes
        newestDir.setLastModified(3000)

        // Total usage: 4 + 8 + 6 + 2 = 20 bytes (>= target 16)
        // Eviction should trigger. Oldest is 8 bytes.
        // After oldest removed, total is 4 + 6 + 2 = 12 bytes.
        // 12 bytes < 16 bytes target, so eviction stops.
        manager.evictLruUntilWithinBudget(activeSession)

        assertThat(oldestDir.exists()).isFalse()
        assertThat(middleDir.exists()).isTrue()
        assertThat(newestDir.exists()).isTrue()
        assertThat(activeSession.exists()).isTrue()
    }

    @Test
    fun evictLruUntilWithinBudget_doesNotTouchActiveSession() {
        val manager = TimeshiftDiskManager(mockContext, maxBudgetBytes = 20L) // Target 16L

        val activeSession = File(timeshiftDir, "activeSession").apply { mkdirs() }
        File(activeSession, "active.txt").apply { writeText("12345678901234567890") } // 20 bytes
        activeSession.setLastModified(500) // very old

        // Usage is 20, > target 16. But activeSession should not be evicted.
        manager.evictLruUntilWithinBudget(activeSession)

        assertThat(activeSession.exists()).isTrue()
    }
}
