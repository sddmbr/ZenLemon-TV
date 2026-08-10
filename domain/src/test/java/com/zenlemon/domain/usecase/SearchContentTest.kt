package com.zenlemon.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.zenlemon.domain.manager.ProviderSyncStateReader
import com.zenlemon.domain.model.Channel
import com.zenlemon.domain.model.Movie
import com.zenlemon.domain.model.Series
import com.zenlemon.domain.model.SyncState
import com.zenlemon.domain.repository.SearchRepository
import com.zenlemon.domain.repository.SearchRepositoryResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SearchContentTest {

    @Test
    fun returnsCombinedResultsAcrossAllSections() = runTest {
        val useCase = SearchContent(
            searchRepository = FakeSearchRepository(
                channelResults = listOf(Channel(id = 1L, name = "News 1")),
                movieResults = listOf(Movie(id = 2L, name = "Movie 1")),
                seriesResults = listOf(Series(id = 3L, name = "Series 1"))
            ),
            providerSyncStateReader = FakeProviderSyncStateReader()
        )

        val result = useCase(providerId = 99L, query = "star").first()

        assertThat(result.channels).hasSize(1)
        assertThat(result.movies).hasSize(1)
        assertThat(result.series).hasSize(1)
    }

    @Test
    fun restrictsResultsToRequestedScope() = runTest {
        val useCase = SearchContent(
            searchRepository = FakeSearchRepository(
                channelResults = listOf(Channel(id = 1L, name = "News 1")),
                movieResults = listOf(Movie(id = 2L, name = "Movie 1")),
                seriesResults = listOf(Series(id = 3L, name = "Series 1"))
            ),
            providerSyncStateReader = FakeProviderSyncStateReader()
        )

        val result = useCase(
            providerId = 99L,
            query = "star",
            scope = SearchContentScope.MOVIES
        ).first()

        assertThat(result.channels).isEmpty()
        assertThat(result.movies).hasSize(1)
        assertThat(result.series).isEmpty()
    }

    @Test
    fun shortQueriesReturnEmptyResults() = runTest {
        val useCase = SearchContent(
            searchRepository = FakeSearchRepository(
                channelResults = listOf(Channel(id = 1L, name = "News 1")),
                movieResults = listOf(Movie(id = 2L, name = "Movie 1")),
                seriesResults = listOf(Series(id = 3L, name = "Series 1"))
            ),
            providerSyncStateReader = FakeProviderSyncStateReader()
        )

        val result = useCase(providerId = 99L, query = "a").first()

        assertThat(result.channels).isEmpty()
        assertThat(result.movies).isEmpty()
        assertThat(result.series).isEmpty()
    }

    @Test
    fun marksSearchPartialWhileProviderSyncIsActive() = runTest {
        val useCase = SearchContent(
            searchRepository = FakeSearchRepository(movieResults = listOf(Movie(id = 2L, name = "Movie 1"))),
            providerSyncStateReader = FakeProviderSyncStateReader(SyncState.Syncing("Indexing movies"))
        )

        val result = useCase(providerId = 99L, query = "movie", scope = SearchContentScope.MOVIES).first()

        assertThat(result.movies).hasSize(1)
        assertThat(result.isPartialResult).isTrue()
    }

    @Test
    fun marksSearchPartialWhileBackgroundIndexJobIsActive() = runTest {
        val useCase = SearchContent(
            searchRepository = FakeSearchRepository(movieResults = listOf(Movie(id = 2L, name = "Movie 1"))),
            providerSyncStateReader = FakeProviderSyncStateReader(backgroundIndexingActive = true)
        )

        val result = useCase(providerId = 99L, query = "movie", scope = SearchContentScope.MOVIES).first()

        assertThat(result.movies).hasSize(1)
        assertThat(result.isPartialResult).isTrue()
    }

    @Test
    fun rethrows_non_io_upstream_failures() = runTest {
        val expected = IllegalStateException("channel search failed")
        val useCase = SearchContent(
            searchRepository = FakeSearchRepository(
                channelFlow = flow { throw expected }
            ),
            providerSyncStateReader = FakeProviderSyncStateReader()
        )

        val thrown = try {
            useCase(providerId = 99L, query = "star").first()
            null
        } catch (error: IllegalStateException) {
            error
        }

        assertThat(thrown).isNotNull()
        assertThat(thrown?.message).isEqualTo(expected.message)
    }

    @Test
    fun marksSearchPartialWhenRepositoryDoesNotEmitWithinBudget() = runTest {
        val useCase = SearchContent(
            searchRepository = FakeSearchRepository(
                searchContentFlow = flow {
                    delay(3_000L)
                    emit(SearchRepositoryResult(movies = listOf(Movie(id = 2L, name = "Late Movie"))))
                }
            ),
            providerSyncStateReader = FakeProviderSyncStateReader()
        )

        val result = useCase(providerId = 99L, query = "movie", scope = SearchContentScope.MOVIES).first()

        assertThat(result.movies).isEmpty()
        assertThat(result.isPartialResult).isTrue()
    }
}

private class FakeProviderSyncStateReader(
    private val state: SyncState = SyncState.Idle,
    private val backgroundIndexingActive: Boolean = false
) : ProviderSyncStateReader {
    override fun currentSyncState(providerId: Long): SyncState = state
    override fun observeBackgroundIndexingActive(providerId: Long): Flow<Boolean> = flowOf(backgroundIndexingActive)
}

private class FakeSearchRepository(
    private val channelResults: List<Channel> = emptyList(),
    private val movieResults: List<Movie> = emptyList(),
    private val seriesResults: List<Series> = emptyList(),
    private val channelFlow: Flow<List<Channel>>? = null,
    private val movieFlow: Flow<List<Movie>>? = null,
    private val seriesFlow: Flow<List<Series>>? = null,
    private val searchContentFlow: Flow<SearchRepositoryResult>? = null
) : SearchRepository {
    override fun searchContent(
        providerId: Long,
        query: String,
        includeLive: Boolean,
        includeMovies: Boolean,
        includeSeries: Boolean,
        maxResultsPerSection: Int
    ): Flow<SearchRepositoryResult> {
        searchContentFlow?.let { return it }
        channelFlow?.let { if (includeLive) return it.map { channels -> SearchRepositoryResult(channels = channels) } }
        movieFlow?.let { if (includeMovies) return it.map { movies -> SearchRepositoryResult(movies = movies) } }
        seriesFlow?.let { if (includeSeries) return it.map { series -> SearchRepositoryResult(series = series) } }

        return flowOf(
            SearchRepositoryResult(
                channels = if (includeLive) channelResults.take(maxResultsPerSection) else emptyList(),
                movies = if (includeMovies) movieResults.take(maxResultsPerSection) else emptyList(),
                series = if (includeSeries) seriesResults.take(maxResultsPerSection) else emptyList()
            )
        )
    }

    override fun searchChannels(providerId: Long, query: String): Flow<List<Channel>> =
        channelFlow ?: flowOf(channelResults)

    override fun searchMovies(providerId: Long, query: String): Flow<List<Movie>> =
        movieFlow ?: flowOf(movieResults)

    override fun searchSeries(providerId: Long, query: String): Flow<List<Series>> =
        seriesFlow ?: flowOf(seriesResults)
}
