package com.jetbrains.kmpapp.screens.detail

import androidx.lifecycle.ViewModel
import com.jetbrains.kmpapp.data.Song
import com.jetbrains.kmpapp.data.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class DetailViewModel(private val musicRepository: MusicRepository) : ViewModel() {
    fun getSong(id: String): Flow<Song?> =
        flowOf(musicRepository.getSong(id))
}
