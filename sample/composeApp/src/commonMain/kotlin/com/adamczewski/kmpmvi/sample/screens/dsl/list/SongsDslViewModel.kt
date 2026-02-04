package com.adamczewski.kmpmvi.sample.screens.dsl.list

import androidx.lifecycle.ViewModel
import com.adamczewski.kmpmvi.mvi.MviContainerHost
import com.adamczewski.kmpmvi.mvi.error.ErrorManager
import com.adamczewski.kmpmvi.mvi.error.toUiError
import com.adamczewski.kmpmvi.mvi.progress.watchProgress
import com.adamczewski.kmpmvi.sample.data.MusicRepository
import com.adamczewski.kmpmvi.sample.screens.list.SongsAction
import com.adamczewski.kmpmvi.sample.screens.list.SongsAction.PulledToRefresh
import com.adamczewski.kmpmvi.sample.screens.list.SongsAction.RetryClicked
import com.adamczewski.kmpmvi.sample.screens.list.SongsAction.SearchQueryChanged
import com.adamczewski.kmpmvi.sample.screens.list.SongsAction.SongSelected
import com.adamczewski.kmpmvi.sample.screens.list.SongsEffect
import com.adamczewski.kmpmvi.sample.screens.list.SongsEffect.OpenSongDetails
import com.adamczewski.kmpmvi.sample.screens.list.SongsState
import com.adamczewski.kmpmvi.sample.utils.onError
import com.adamczewski.kmpmvi.sample.utils.onSuccess
import com.adamczewski.kmpmvi.viewmodel.dsl.viewmodelMvi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SongsDslViewModel(
    private val musicRepository: MusicRepository,
    private val errorManager: ErrorManager,
) : ViewModel(), MviContainerHost<SongsAction, SongsState, SongsEffect> {
    private val searchQuery = MutableStateFlow<String?>(null)

    override val component = viewmodelMvi<SongsAction, SongsState, SongsEffect>(SongsState()) {
        observeError(errorManager) { error ->
            setState { copy(error = error) }
        }

        observeProgress { isLoading ->
            setState { copy(isLoading = isLoading) }
        }

        onInit {
            searchQuery
                .flatMapLatest { query ->
                    musicRepository.getSongs(query = query)
                        .watchProgress(progress, PROGRESS_ID)
                }
                .onSuccess { songs -> setState { copy(songs = songs, error = null) } }
                .onError { errorManager.addError(it.toUiError()) }
                .launchIn(scope)
        }

        actions {
            onAction<PulledToRefresh> {
                progress.addProgress(PROGRESS_ID)
                musicRepository.refresh()
            }

            onAction<RetryClicked> {
                progress.addProgress(PROGRESS_ID)
                musicRepository.refresh()
            }

            onAction<SongSelected> {
                setEffect { OpenSongDetails(it.song.id) }
            }

            onActionFlow<SearchQueryChanged> {
                debounce(300)
                    .map { it.query }
                    .map { query ->
                        if (query.length >= MINIMUM_QUERY_LENGTH) {
                            query
                        } else {
                            null
                        }
                    }
                    .distinctUntilChanged()
                    .onEach { query ->
                        searchQuery.value = query
                    }
            }
        }

        lifecycle {
            onSubscribe {
                println("onSubscribe")
            }

            onUnsubscribe {
                println("onUnsubscribe")
            }
        }
    }

    companion object Companion {
        private const val PROGRESS_ID = "pull_to_refresh"
        private const val MINIMUM_QUERY_LENGTH = 3
    }
}
