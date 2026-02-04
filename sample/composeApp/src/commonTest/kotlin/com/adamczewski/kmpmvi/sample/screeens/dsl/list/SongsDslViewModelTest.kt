package com.adamczewski.kmpmvi.sample.screeens.dsl.list

import com.adamczewski.kmpmvi.sample.data.MusicRepository
import com.adamczewski.kmpmvi.sample.screens.dsl.list.SongsDslViewModel
import com.adamczewski.kmpmvi.mvi.error.ErrorManager
import com.adamczewski.kmpmvi.sample.fixtures.SongsFakeRepository
import com.adamczewski.kmpmvi.sample.fixtures.SongsFakeRepository.Companion.INIT_SONGS
import com.adamczewski.kmpmvi.sample.screens.list.SongsAction
import com.adamczewski.kmpmvi.sample.screens.list.SongsEffect
import com.adamczewski.kmpmvi.test.TEST_DELAY
import com.adamczewski.kmpmvi.test.testState
import com.adamczewski.kmpmvi.test.whenActionThenEffect
import com.adamczewski.kmpmvi.test.whenActionThenShowProgress
import com.adamczewski.kmpmvi.test.whenInitThenShowError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SongsDslViewModelTest {

    @BeforeTest
    fun beforeEach() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun cleanUp() {
        Dispatchers.resetMain()
    }

    private fun createSut(
        repository: MusicRepository = SongsFakeRepository()
    ): SongsDslViewModel {
        return SongsDslViewModel(
            repository, ErrorManager()
        )
    }

    @Test
    fun `when initialized, then fetched all songs`() = runTest {
        createSut().testState(this) {
            assertEquals(INIT_SONGS, expectMostRecentItem().songs)
        }
    }

    @Test
    fun `when initialized, then show and hide loading`() = runTest {
        whenActionThenShowProgress(
            createSut(SongsFakeRepository(requestDelay = TEST_DELAY)),
            stateFieldToAssert = { it.isLoading }
        )
    }

    @Test
    fun `when songs request failed, then show error`() = runTest {
        val repository = SongsFakeRepository(
            initSongs = Result.failure(IllegalStateException("test")),
            requestDelay = TEST_DELAY
        )
        whenInitThenShowError(
            createSut(repository),
            errorFieldProducer = { it.error },
        )
    }

    @Test
    fun `when pulled to refresh, then refresh with loading`() = runTest {
        val repository = SongsFakeRepository(requestDelay = TEST_DELAY)

        whenActionThenShowProgress(
            stateComponent = createSut(repository),
            beforeActionBlock = {
                advanceUntilIdle()
                assertEquals(0, repository.refreshCount)
            },
            actionToSubmit = SongsAction.PulledToRefresh,
            stateFieldToAssert = { it.isLoading }
        )
        assertEquals(1, repository.refreshCount)
    }

    @Test
    fun `when song clicked, then open song details`() = runTest {
        whenActionThenEffect(
            createSut(),
            actionToSubmit = SongsAction.SongSelected(INIT_SONGS[0]),
            expectedEffect = SongsEffect.OpenSongDetails(INIT_SONGS[0].id),
        )
    }

    @Test
    fun `when query changed, then search songs`() = runTest {
        val repository = SongsFakeRepository()
        createSut(repository).testState(this) {
            assertEquals(1, repository.searchCount) // initial request
            submitAction(SongsAction.SearchQueryChanged("Song 1"))
            advanceTimeBy(301) // debounce

            assertEquals(listOf(INIT_SONGS[0]), expectMostRecentItem().songs)
            assertEquals(2, repository.searchCount)
        }
    }

    @Test
    fun `when query changed rapidly, then debounce query and perform search once`() = runTest {
        val repository = SongsFakeRepository()
        createSut(repository).testState(this) {
            assertEquals(1, repository.searchCount) // initial request

            submitAction(SongsAction.SearchQueryChanged("Son"))
            advanceTimeBy(100)
            submitAction(SongsAction.SearchQueryChanged("Song 2"))
            advanceTimeBy(100)
            submitAction(SongsAction.SearchQueryChanged("Song 1"))
            advanceTimeBy(301)

            assertEquals(listOf(INIT_SONGS[0]), expectMostRecentItem().songs)
            assertEquals(2, repository.searchCount)
        }
    }
}
