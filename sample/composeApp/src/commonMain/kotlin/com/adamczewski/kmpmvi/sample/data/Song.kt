package com.adamczewski.kmpmvi.sample.data

import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val id: String,
    val title: String,
    val artistDisplayName: String,
    val releaseDate: String,
)
