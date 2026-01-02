@file:OptIn(ExperimentalMaterial3Api::class)

package com.jetbrains.kmpapp.screens.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamczewski.kmp.compose.handleEffects
import com.jetbrains.kmpapp.data.Song
import com.jetbrains.kmpapp.screens.EmptyScreenContent
import com.zumba.consumerapp.ui.utils.collectAsStateWithLifecycle
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SongsScreen(
    navigateToDetails: (songId: String) -> Unit
) {
    val viewModel = koinViewModel<SongsViewModel>()
    val state by viewModel.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) {
        SongsScreen(state, viewModel::submitAction)
    }

    viewModel.handleEffects { effect ->
        when (effect) {
            is SongsEffect.OpenSongDetails -> navigateToDetails(effect.songId)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.submitAction(SongsAction.Init)
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

@Composable
private fun SongsScreen(
    state: SongsState,
    submitAction: (SongsAction) -> Unit
) {
    PullToRefreshBox(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        isRefreshing = state.isLoading,
        onRefresh = { submitAction(SongsAction.PulledToRefresh) }
    ) {
        if (state.songs == null) return@PullToRefreshBox

        Column(Modifier.padding(horizontal = 16.dp)) {
            SearchInput(submitAction)
            Spacer(Modifier.height(8.dp))
            AnimatedContent(state.showEmptyState) { showEmptyState ->
                if (showEmptyState) {
                    EmptyScreenContent(
                        modifier = Modifier.fillMaxSize(),
                        onRefreshClick = { submitAction(SongsAction.PulledToRefresh) }
                    )
                } else {
                    SongsGrid(
                        songs = state.songs,
                        onSongClick = {
                            submitAction(SongsAction.SongSelected(it))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchInput(submitAction: (SongsAction) -> Unit) {
    var text by remember { mutableStateOf("") }
    LaunchedEffect(text) {
        submitAction(SongsAction.SearchQueryChanged(text))
    }
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = text,
        onValueChange = { text = it },
        shape = MaterialTheme.shapes.large,
        label = { Text("Search songs") },
    )
}

@Composable
private fun SongsGrid(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns = GridCells.Adaptive(180.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(songs, key = { it.id }) { song ->
            SongItem(
                song = song,
                onClick = { onSongClick(song) },
            )
        }
    }
}

@Composable
private fun SongItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        Modifier.clickable { onClick() }
    ) {
        Column(
            modifier.padding(8.dp)
        ) {
            Text(song.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(song.artistDisplayName, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview
@Composable
private fun SongsScreenPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SongsScreen(
            state = SongsState(
                isLoading = false,
                songs = listOf(
                    Song(
                        id = "1",
                        title = "Song 1",
                        artistDisplayName = "Artist 1",
                        releaseDate = "2025-12-18"
                    ),
                    Song(
                        id = "2",
                        title = "Song 2",
                        artistDisplayName = "Artist 2",
                        releaseDate = "2025-12-18"
                    ),
                )
            ),
            submitAction = {}
        )
    }
}
