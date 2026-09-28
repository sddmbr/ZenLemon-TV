package com.zenlemon.data.repository

import com.google.common.truth.Truth.assertThat
import com.zenlemon.data.local.DatabaseTransactionRunner
import com.zenlemon.data.local.dao.FavoriteDao
import com.zenlemon.data.local.dao.VirtualGroupDao
import com.zenlemon.data.local.entity.FavoriteEntity
import com.zenlemon.data.local.entity.VirtualGroupEntity
import com.zenlemon.domain.model.ContentType
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.system.measureTimeMillis

class FavoriteRepositoryPerformanceTest {

    private val favoriteDao: FavoriteDao = mock()
    private val virtualGroupDao: VirtualGroupDao = mock()
    private val transactionRunner = object : DatabaseTransactionRunner {
        override suspend fun <T> inTransaction(block: suspend () -> T): T = block()
    }

    private val repository = FavoriteRepositoryImpl(
        favoriteDao = favoriteDao,
        virtualGroupDao = virtualGroupDao,
        transactionRunner = transactionRunner
    )

    @Test
    fun benchmarkMergeGroupInto() = runTest {
        val size = 5000
        val sourceFavorites = (1..size).map { i ->
            FavoriteEntity(
                id = i.toLong(),
                providerId = 7L,
                contentId = 100L + i,
                contentType = ContentType.LIVE,
                position = i,
                groupId = 11L,
                groupKey = 11L,
                addedAt = 1_000L
            )
        }
        val targetFavorites = sourceFavorites.take(100).map {
            it.copy(id = it.id + size, groupId = 12L, groupKey = 12L)
        }

        whenever(virtualGroupDao.getById(11L)).thenReturn(
            VirtualGroupEntity(id = 11L, providerId = 7L, name = "Source", contentType = ContentType.LIVE)
        )
        whenever(virtualGroupDao.getById(12L)).thenReturn(
            VirtualGroupEntity(id = 12L, providerId = 7L, name = "Target", contentType = ContentType.LIVE)
        )
        whenever(favoriteDao.getByGroup(11L)).thenReturn(flowOf(sourceFavorites))
        whenever(favoriteDao.getByGroupSync(11L)).thenReturn(sourceFavorites)
        whenever(favoriteDao.getByGroupSync(12L)).thenReturn(targetFavorites)
        whenever(favoriteDao.get(any(), any(), any(), any())).thenReturn(null)

        val time = measureTimeMillis {
            repository.mergeGroupInto(11L, 12L)
        }
        println("MergeGroupInto with $size favorites took $time ms")
    }

    @Test
    fun benchmarkDeleteGroup() = runTest {
        val size = 5000
        val groupFavorites = (1..size).map { i ->
            FavoriteEntity(
                id = i.toLong(),
                providerId = 7L,
                contentId = 100L + i,
                contentType = ContentType.LIVE,
                position = i,
                groupId = 11L,
                groupKey = 11L,
                addedAt = 1_000L
            )
        }
        val globalFavorites = groupFavorites.take(100).map {
            it.copy(id = it.id + size, groupId = null, groupKey = 0L)
        }

        whenever(virtualGroupDao.getById(11L)).thenReturn(
            VirtualGroupEntity(id = 11L, providerId = 7L, name = "Source", contentType = ContentType.LIVE)
        )
        whenever(favoriteDao.getByGroup(11L)).thenReturn(flowOf(groupFavorites))
        whenever(favoriteDao.getByGroupSync(11L)).thenReturn(groupFavorites)
        whenever(favoriteDao.getGlobalByTypeSync(7L, ContentType.LIVE.name)).thenReturn(globalFavorites)
        whenever(favoriteDao.get(any(), any(), any(), any())).thenReturn(null)

        val time = measureTimeMillis {
            repository.deleteGroup(11L)
        }
        println("DeleteGroup with $size favorites took $time ms")
    }
}
