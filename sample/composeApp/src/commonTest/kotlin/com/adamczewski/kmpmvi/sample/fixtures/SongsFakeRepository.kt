package com.adamczewski.kmpmvi.sample.fixtures

import com.adamczewski.kmpmvi.sample.data.MusicRepository
import com.adamczewski.kmpmvi.sample.data.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

public class SongsFakeRepository(
    requestDelay: Long = 0,
    initSongs: Result<List<Song>> = Result.success(INIT_SONGS)
) : MusicRepository {
    private val refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val songs: Flow<Result<List<Song>>> = merge(
        flow {
            println("before delay flow")
            delay(requestDelay)
            println("after delay flow")
            emit(initSongs)
        },
        refresh.map {
            delay(requestDelay)
            Result.success(REFRESHED_SONGS)
        })

    var refreshCount = 0
        private set
    var searchCount = 0
        private set

    override fun getSongs(query: String?): Flow<Result<List<Song>>> {
        searchCount++
        if (query == null) return songs
        return songs.map { result ->
            result.map {
                it.filter { it.title.contains(query) }
            }
        }
    }

    override fun getSong(id: String): Result<Song?> {
        throw NotImplementedError()
    }

    override suspend fun refresh() {
        refreshCount++
        refresh.emit(Unit)
    }

    companion object Companion {
        val INIT_SONGS: List<Song> = listOf(
            Song(
                id = "1",
                title = "Song 1 test",
                artistDisplayName = "Artist 1",
                releaseDate = "2025-12-18"
            ),
            Song(
                id = "2",
                title = "Song 2 test",
                artistDisplayName = "Artist 2",
                releaseDate = "2025-12-19"
            )
        )
        private val REFRESHED_SONGS: List<Song> = INIT_SONGS.reversed()
    }
}