package com.jetbrains.kmpapp.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.Song
import com.jetbrains.kmpapp.data.MusicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ListViewModel(musicRepository: MusicRepository) : ViewModel() {
    val objects: StateFlow<List<Song>> =
        musicRepository.getSongs()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
