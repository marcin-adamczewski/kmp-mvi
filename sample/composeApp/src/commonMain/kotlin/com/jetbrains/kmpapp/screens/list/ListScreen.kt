package com.jetbrains.kmpapp.screens.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jetbrains.kmpapp.data.Song
import com.jetbrains.kmpapp.screens.EmptyScreenContent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ListScreen(
    navigateToDetails: (songId: String) -> Unit
) {
    val viewModel = koinViewModel<ListViewModel>()
    val songs by viewModel.objects.collectAsStateWithLifecycle()

    AnimatedContent(songs.isNotEmpty()) { objectsAvailable ->
        if (objectsAvailable) {
            SongsGrid(
                songs = songs,
                onSongClick = navigateToDetails,
            )
        } else {
            EmptyScreenContent(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun SongsGrid(
    songs: List<Song>,
    onSongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(180.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
    ) {
        items(songs, key = { it.id }) { song ->
            SongItem(
                song = song,
                onClick = { onSongClick(song.id) },
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
    Column(
        modifier
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Text(song.title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(song.artistDisplayName, style = MaterialTheme.typography.bodyMedium)
    }
}
