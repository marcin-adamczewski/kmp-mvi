package com.adamczewski.kmpmvi.sample.screens.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.adamczewski.kmpmvi.sample.data.Song
import com.zumba.consumerapp.ui.utils.collectAsStateWithLifecycle
import kmp_mvi.sample.composeapp.generated.resources.Res
import kmp_mvi.sample.composeapp.generated.resources.back
import kmp_mvi.sample.composeapp.generated.resources.label_artist
import kmp_mvi.sample.composeapp.generated.resources.label_date
import kmp_mvi.sample.composeapp.generated.resources.label_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SongDetailsScreen(
    songId: String,
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<SongDetailViewModel>()
    val state: SongDetailState by viewModel.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.submitAction(SongDetailsAction.Init(songId))
    }

    state.song?.let { song ->
        SongDetailsScreen(song, onBackClick = navigateBack)
    }
}

@Composable
private fun SongDetailsScreen(
    song: Song,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back))
                    }
                }
            )
        },
        modifier = modifier.windowInsetsPadding(WindowInsets.systemBars),
    ) { paddingValues ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            SelectionContainer {
                Column(Modifier.padding(12.dp)) {
                    Text(song.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(6.dp))
                    LabeledInfo(stringResource(Res.string.label_title), song.title)
                    LabeledInfo(stringResource(Res.string.label_artist), song.artistDisplayName)
                    LabeledInfo(stringResource(Res.string.label_date), song.releaseDate)
                }
            }
        }
    }
}

@Composable
private fun LabeledInfo(
    label: String,
    data: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(vertical = 4.dp)) {
        Spacer(Modifier.height(6.dp))
        Text(
            buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("$label: ")
                }
                append(data)
            }
        )
    }
}
