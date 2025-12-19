package com.jetbrains.kmpapp.screens.detail

import com.adamczewski.kmpmvi.mvi.MviAction
import com.adamczewski.kmpmvi.mvi.MviState
import com.adamczewski.kmpmvi.mvi.NoEffects
import com.adamczewski.kmpmvi.mvi.actions.ActionsManager
import com.adamczewski.kmpmvi.mvi.android.MviViewModel
import com.adamczewski.kmpmvi.mvi.error.UiError
import com.adamczewski.kmpmvi.mvi.error.toUiError
import com.jetbrains.kmpapp.data.Song
import com.jetbrains.kmpapp.data.MusicRepository
import com.jetbrains.kmpapp.screens.detail.SongDetailsAction.*

class SongDetailViewModel(private val musicRepository: MusicRepository) :
    MviViewModel<SongDetailsAction, SongDetailState, NoEffects>(SongDetailState()) {

    override fun ActionsManager<SongDetailsAction>.handleActions() {
        onActionSingle<Init> {
            musicRepository.getSong(it.songId)
                .onSuccess { song ->
                    setState { copy(song = song, error = null) }
                }
                .onFailure { throwable ->
                    setState { copy(error = throwable.toUiError())}
                }
        }
    }
}

data class SongDetailState(
    val song: Song? = null,
    val error: UiError? = null
) : MviState

sealed interface SongDetailsAction : MviAction {
    data class Init(val songId: String) : SongDetailsAction
}
