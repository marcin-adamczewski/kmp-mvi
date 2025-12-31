package com.jetbrains.kmpapp.screens.list

import com.adamczewski.kmp.viewmodel.MviViewModel
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.actions.ActionsManager
import com.adamczewski.kmpmvi.mvi.error.BaseErrorManager
import com.adamczewski.kmpmvi.mvi.error.ErrorManager
import com.adamczewski.kmpmvi.mvi.error.UiError
import com.adamczewski.kmpmvi.mvi.error.toUiError
import com.adamczewski.kmpmvi.mvi.progress.watchProgress
import com.jetbrains.kmpapp.data.MusicRepository
import com.jetbrains.kmpapp.data.Song
import com.jetbrains.kmpapp.screens.list.SongsAction.Init
import com.jetbrains.kmpapp.screens.list.SongsAction.PulledToRefresh
import com.jetbrains.kmpapp.screens.list.SongsAction.RetryClicked
import com.jetbrains.kmpapp.screens.list.SongsAction.SearchQueryChanged
import com.jetbrains.kmpapp.screens.list.SongsAction.SongSelected
import com.jetbrains.kmpapp.screens.list.SongsEffect.OpenSongDetails
import com.jetbrains.kmpapp.utils.onError
import com.jetbrains.kmpapp.utils.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
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
