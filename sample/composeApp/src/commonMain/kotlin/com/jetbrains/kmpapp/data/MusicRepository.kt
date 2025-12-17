package com.jetbrains.kmpapp.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class MusicRepository() {
    fun getSongs(): Flow<List<Song>> = flowOf(sampleSongs)

    fun getSong(id: String): Song? = sampleSongs.find { it.id == id }
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
    Song(id = "20", title = "Solar Power", artistDisplayName = "Lorde", releaseDate = "2025-12-18")
)
