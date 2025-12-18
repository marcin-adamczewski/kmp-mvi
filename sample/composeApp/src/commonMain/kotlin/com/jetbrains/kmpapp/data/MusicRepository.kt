package com.jetbrains.kmpapp.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onSubscription
import kotlin.random.Random

class MusicRepository() {
    private val refresh = MutableSharedFlow<Unit>()
    private val random = Random(100)

    fun getSongs(query: String? = null): Flow<Result<List<Song>>> = refresh
        .onSubscription { emit(Unit) }
        .flatMapLatest {
            val randomInt = random.nextInt(6)
            delay(800)
            val songs = if (query != null) {
                sampleSongs.shuffled().filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.artistDisplayName.contains(query, ignoreCase = true)
                }
            } else {
                when (randomInt) {
                    0 -> emptyList()
                    else -> sampleSongs.shuffled()
                }
            }

            if (randomInt == 1) {
                flowOf(Result.failure(IllegalStateException("Simulated error")))
            } else {
                flowOf(Result.success(songs))
            }

        }
        .catch { error ->
            if (error is CancellationException) {
                throw error
            } else {
                emit(Result.failure(error))
            }
        }

    fun getSong(id: String): Result<Song?> =
        runCatching { sampleSongs.find { it.id == id } }

    suspend fun refresh() {
        refresh.emit(Unit)
    }
}

private val sampleSongs = listOf(
    Song(id = "1", title = "Midnight City", artistDisplayName = "M83", releaseDate = "2025-12-18"),
    Song(id = "2", title = "Starboy", artistDisplayName = "The Weeknd", releaseDate = "2025-12-18"),
    Song(
        id = "3",
        title = "Blinding Lights",
        artistDisplayName = "The Weeknd",
        releaseDate = "2025-12-18"
    ),
    Song(
        id = "4",
        title = "Level of Concern",
        artistDisplayName = "Twenty One Pilots",
        releaseDate = "2025-12-18"
    ),
    Song(
        id = "5",
        title = "Heat Waves",
        artistDisplayName = "Glass Animals",
        releaseDate = "2025-12-18"
    ),
    Song(
        id = "6",
        title = "Circles",
        artistDisplayName = "Post Malone",
        releaseDate = "2025-12-18"
    ),
    Song(
        id = "7",
        title = "Levitating",
        artistDisplayName = "Dua Lipa",
        releaseDate = "2025-12-18"
    ),
    Song(
        id = "8",
        title = "Don't Start Now",
        artistDisplayName = "Dua Lipa",
        releaseDate = "2025-12-18"
    ),
    Song(
        id = "9",
        title = "Stay",
        artistDisplayName = "The Kid LAROI & Justin Bieber",
        releaseDate = "2025-12-18"
    ),
    Song(
        id = "10",
        title = "Bad Habits",
        artistDisplayName = "Ed Sheeran",
        releaseDate = "2025-12-18"
    ),
    Song(
        id = "11",
        title = "Shivers",
        artistDisplayName = "Ed Sheeran",
        releaseDate = "2025-12-18"
    ),
    Song(
        id = "12",
        title = "As It Was",
        artistDisplayName = "Harry Styles",
        releaseDate = "2025-12-18"
    ),
    Song(
        id = "13",
        title = "Watermelon Sugar",
        artistDisplayName = "Harry Styles",
        releaseDate = "2025-12-18"
    ),
    Song(
        id = "14",
        title = "Good 4 U",
        artistDisplayName = "Olivia Rodrigo",
        releaseDate = "2025-12-18"
    ),
    Song(
        id = "15",
        title = "Drivers License",
        artistDisplayName = "Olivia Rodrigo",
        releaseDate = "2025-12-18"
    ),
    Song(
        id = "16",
        title = "Peaches",
        artistDisplayName = "Justin Bieber",
        releaseDate = "2025-12-18"
    ),
    Song(
        id = "17",
        title = "Montero (Call Me By Your Name)",
        artistDisplayName = "Lil Nas X",
        releaseDate = "2025-12-18"
    ),
    Song(
        id = "18",
        title = "Industry Baby",
        artistDisplayName = "Lil Nas X",
        releaseDate = "2025-12-18"
    ),
    Song(
        id = "19",
        title = "Save Your Tears",
        artistDisplayName = "The Weeknd",
        releaseDate = "2025-12-18"
    ),
    Song(id = "20", title = "Solar Power", artistDisplayName = "Lorde", releaseDate = "2025-12-18"),
    Song(id = "21", title = "Solar Power", artistDisplayName = "Lorde", releaseDate = "2025-12-18"),
    Song(id = "22", title = "Solar Power", artistDisplayName = "Lorde", releaseDate = "2025-12-18"),
    Song(id = "23", title = "Solar Power", artistDisplayName = "Lorde", releaseDate = "2025-12-18"),
    Song(id = "24", title = "Solar Power", artistDisplayName = "Lorde", releaseDate = "2025-12-18")

)
