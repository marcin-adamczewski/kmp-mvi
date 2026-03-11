package com.adamczewski.kmpmvi.sample.screens.dsl.list

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.adamczewski.kmpmvi.compose.ConsumeEffects
import com.adamczewski.kmpmvi.compose.collectAsStateWithLifecycle
import com.adamczewski.kmpmvi.sample.screens.list.SongsAction
import com.adamczewski.kmpmvi.sample.screens.list.SongsEffect
import com.adamczewski.kmpmvi.sample.screens.list.SongsScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SongsDslScreen(
    navigateToDetails: (songId: String) -> Unit,
    viewModel: SongsDslViewModel = koinViewModel<SongsDslViewModel>()
) {
    val state by viewModel.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) {
        SongsScreen(state, viewModel::submitAction)
    }

    viewModel.ConsumeEffects { effect ->
        when (effect) {
            is SongsEffect.OpenSongDetails -> navigateToDetails(effect.songId)
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(
                message = "Something went wrong",
                duration = SnackbarDuration.Long
            )
        }
    }
}