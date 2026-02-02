package com.adamczewski.kmpmvi.sample.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamczewski.kmpmvi.mvi.dsl.MviActionScope
import com.adamczewski.kmpmvi.viewmodel.MviViewModel
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.actions.ActionsManager
import com.adamczewski.kmpmvi.mvi.MviContainerHost
import com.adamczewski.kmpmvi.mvi.error.ErrorManager
import com.adamczewski.kmpmvi.mvi.error.UiError
import com.adamczewski.kmpmvi.mvi.error.toUiError
import com.adamczewski.kmpmvi.mvi.dsl.mvi
import com.adamczewski.kmpmvi.mvi.progress.watchProgress
import com.adamczewski.kmpmvi.sample.data.MusicRepository
import com.adamczewski.kmpmvi.sample.data.Song
import com.adamczewski.kmpmvi.sample.screens.list.SongsAction.Init
import com.adamczewski.kmpmvi.sample.screens.list.SongsAction.PulledToRefresh
import com.adamczewski.kmpmvi.sample.screens.list.SongsAction.RetryClicked
import com.adamczewski.kmpmvi.sample.screens.list.SongsAction.SearchQueryChanged
import com.adamczewski.kmpmvi.sample.screens.list.SongsAction.SongSelected
import com.adamczewski.kmpmvi.sample.screens.list.SongsEffect.OpenSongDetails
import com.adamczewski.kmpmvi.sample.utils.onError
import com.adamczewski.kmpmvi.sample.utils.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class SongsViewModel(
    private val musicRepository: MusicRepository,
    private val errorManager: ErrorManager,
) : MviViewModel<SongsAction, SongsState, SongsEffect>(
    SongsState()
) {
    private val searchQuery = MutableStateFlow<String?>(null)

    init {
        observeError(errorManager) { error ->
            setState { copy(error = error) }
        }

        observeProgress { isLoading ->
            setState { copy(isLoading = isLoading) }
        }
    }

    override fun ActionsManager<SongsAction>.handleActions() {
        onActionFlowSingle<Init> {
            searchQuery.flatMapLatest { query ->
                musicRepository.getSongs(query = query)
                    .watchProgress(progress, PULL_TO_REFRESH_ID)
                    .onSuccess { songs ->
                        setState { copy(songs = songs, error = null) }
                    }
                    .onError { errorManager.addError(it.toUiError()) }
            }
        }

        onAction<PulledToRefresh> {
            progress.addProgress(PULL_TO_REFRESH_ID)
            musicRepository.refresh()
        }

        onAction<RetryClicked> {
            progress.addProgress(PULL_TO_REFRESH_ID)
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

    companion object {
        private const val PULL_TO_REFRESH_ID = "pull_to_refresh"
        private const val MINIMUM_QUERY_LENGTH = 3
    }
}

data class SongsState(
    val isLoading: Boolean = true,
    val error: UiError? = null,
    val songs: List<Song>? = null
) : MviState {
    val showEmptyState: Boolean = songs?.isEmpty() == true
}

sealed interface SongsEffect : MviEffect {
    data class OpenSongDetails(val songId: String) : SongsEffect
}

sealed interface SongsAction : MviAction {
    data object Init : SongsAction
    data class SongSelected(val song: Song) : SongsAction
    data object PulledToRefresh : SongsAction
    data object RetryClicked : SongsAction
    data class SearchQueryChanged(val query: String) : SongsAction
}

class SongsViewModel2(
    private val musicRepository: MusicRepository,
    private val errorManager: ErrorManager,
) : ViewModel(), MviContainerHost<SongsAction, SongsState, SongsEffect> {
    private val searchQuery = MutableStateFlow<String?>(null)

    override val component = mvi<SongsAction, SongsState, SongsEffect>(
        initialState = SongsState(),
        scope = viewModelScope
    ) {
        onInit {
            searchQuery.flatMapLatest { query ->
                musicRepository.getSongs(query = query)
                    .watchProgress(progress, PULL_TO_REFRESH_ID)
                    .onSuccess { songs -> setState { copy(songs = songs, error = null) } }
                    .onError { errorManager.addError(it.toUiError()) }
            }.launchIn(scope)

            observeError(errorManager) { error ->
                setState { copy(error = error) }
            }

            observeProgress { isLoading ->
                setState { copy(isLoading = isLoading) }
            }
        }

        lifecycle {
            onSubscribe {

            }

            onUnsubscribe {

            }
        }

        actions {
            onAction<PulledToRefresh> {
                progress.addProgress(PULL_TO_REFRESH_ID)
                musicRepository.refresh()
            }

            onAction<RetryClicked> {
                progress.addProgress(PULL_TO_REFRESH_ID)
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
    }

    private fun MviActionScope<SongsState, *, *>.fetchData() {
        searchQuery.flatMapLatest { query ->
            musicRepository.getSongs(query = query)
                .watchProgress(progress, PULL_TO_REFRESH_ID)
                .onSuccess { songs ->
                    setState { copy(songs = songs, error = null) }
                }
                .onError { errorManager.addError(it.toUiError()) }
        }.launchIn(scope)
    }

    companion object Companion {
        private const val PULL_TO_REFRESH_ID = "pull_to_refresh"
        private const val MINIMUM_QUERY_LENGTH = 3
    }
}
