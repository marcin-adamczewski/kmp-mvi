# kmp-mvi

A lightweight, flexible, and powerful MVI (Model-View-Intent) library for Kotlin Multiplatform. Designed to simplify state management in your KMP projects with first-class support for Coroutines, Flow, and Compose.

## Features

- **Multiplatform**: Works on Android, iOS, JVM, Wasm, and Linux.
- **Coroutines & Flow based**: Built on top of Kotlin's reactive primitives.
- **MviViewModel**: Seamless integration with `androidx.lifecycle.ViewModel` for automatic lifecycle management.
- **Side Effects**: Robust handling of one-time events (effects) like navigation or toast messages.
- **Progress Management**: Easy-to-use API for tracking loading states across multiple operations.
- **Error Management**: Centralized error handling and propagation to the UI.
- **Logging**: Built-in support for logging state transitions, actions, and effects.
- **Compose Support**: Dedicated extensions for state collection and effect handling in Jetpack and Multiplatform Compose.

## Installation

Add the following to your `build.gradle.kts` in your KMP project:

```kotlin
repositories {
    mavenCentral()
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Core MVI components
            implementation("com.adamczewski.kmpmvi:core:1.0.0-alpha9")
            
            // ViewModel integration (recommended for Android/KMP apps)
            implementation("com.adamczewski.kmpmvi:viewmodel:1.0.0-alpha9")
            
            // Compose extensions (for Jetpack/Multiplatform Compose)
            implementation("com.adamczewski.kmpmvi:compose:1.0.0-alpha9")
        }
    }
}
```

## Usage

### 1. Define your MVI Components

Define your state, actions, and effects. They should implement `MviState`, `MviAction`, and `MviEffect` respectively.

```kotlin
data class SongsState(
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val error: String? = null
) : MviState

sealed interface SongsAction : MviAction {
    data object Init : SongsAction
    data class SearchQueryChanged(val query: String) : SongsAction
    data class SongSelected(val song: Song) : SongsAction
}

sealed interface SongsEffect : MviEffect {
    data class OpenSongDetails(val songId: String) : SongsEffect
}
```

### 2. Create your ViewModel

Extend `MviViewModel` and implement `handleActions()`. This is where your business logic resides.

The library features a built-in lifecycle management system based on the number of active subscribers to the state flow. You can react to these changes using `onInit`, `onSubscribe`, and `onUnsubscribe` callbacks.

```kotlin
class SongsViewModel(
    private val repository: MusicRepository
) : MviViewModel<SongsAction, SongsState, SongsEffect>(
    initialState = SongsState()
) {

    init {
        // Called once when the first subscriber connects to the state
        onInit { 
             // perform one-time initialization
        }

        // Called whenever the subscriber count goes from 0 to 1
        onSubscribe { 
            // start observing external data sources
        }

        // Called whenever the subscriber count hits 0
        onUnsubscribe { 
            // stop observing to save resources
        }
    }

    override fun ActionsManager<SongsAction>.handleActions() {
        // Handle single actions
        onActionSingle<SongsAction.Init> {
            setState { copy(isLoading = true) }
            try {
                val result = repository.getSongs()
                setState { copy(isLoading = false, songs = result) }
            } catch (e: Exception) {
                setState { copy(isLoading = false, error = e.message) }
            }
        }

        // Handle actions and emit effects
        onAction<SongsAction.SongSelected> { action ->
            setEffect { SongsEffect.OpenSongDetails(action.song.id) }
        }
        
        // Handle continuous action flows (e.g. search query debouncing)
        onActionFlow<SongsAction.SearchQueryChanged> {
            debounce(300)
                .distinctUntilChanged()
                .onEach { action ->
                    val result = repository.searchSongs(action.query)
                    setState { copy(songs = result) }
                }
        }
    }
}
```

> **Note**: The lifecycle of the MVI component is automatically managed. When using `collectAsStateWithLifecycle()` in Compose, it will trigger `onSubscribe` when the screen enters the foreground and `onUnsubscribe` when it leaves, allowing for efficient resource management.

### 3. Use in Compose

Connect your UI with the ViewModel using the provided extensions.

```kotlin
@Composable
fun SongsScreen(viewModel: SongsViewModel) {
    // Collect state with lifecycle awareness
    val state by viewModel.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    // Handle one-time side effects
    viewModel.handleEffects { effect ->
        when (effect) {
            is SongsEffect.OpenSongDetails -> { /* navigate to details */ }
        }
    }

    Box {
        if (state.isLoading) {
            CircularProgressIndicator()
        }
        
        TextField(
            value = searchQuery,
            onValueChange = { 
                searchQuery = it
                viewModel.submitAction(SongsAction.SearchQueryChanged(it)) 
            }
        )
        
        state.songs.forEach { song ->
            Text(
                text = song.title,
                modifier = Modifier.clickable { 
                    viewModel.submitAction(SongsAction.SongSelected(song)) 
                }
            )
        }
    }
}
```

## Advanced Features

### Progress Tracking

The library provides a built-in `ProgressManager` to track loading states easily.

```kotlin
// In your ViewModel
init {
    observeProgress { isLoading ->
        setState { copy(isLoading = isLoading) }
    }
}

// In handleActions
onAction<SongsAction.Init> {
    // Automatically manage loading state during the block
    withProgress {
        val songs = repository.getSongs()
        setState { copy(songs = songs) }
    }
}
```

### Logging

You can enable logging to track all actions, state changes, effects, and lifecycle events in your console. This is extremely helpful for debugging complex state transitions and verifying behavior in both code and tests.

```kotlin
class SongsViewModel(...) : MviViewModel<...>(...) {
    override fun settings() = buildSettings {
        isLoggerEnabled = true
    }
}
```

Example log output:

```text
SongsViewModel@021ba2c6: [Initial State] - SongsState(isLoading=true, error=null, songs=null)
SongsViewModel@021ba2c6: [Action] - Init
SongsViewModel@021ba2c6: [Lifecycle] - onInit
SongsViewModel@021ba2c6: [Lifecycle] - onSubscribe
SongsViewModel@021ba2c6: [State] - SongsState(isLoading=false, error=null, songs=[Song(id=1, title=Midnight City, artistDisplayName=M83, releaseDate=2025-12-18)])
SongsViewModel@021ba2c6: [Action] - SearchQueryChanged(query=)
SongsViewModel@021ba2c6: [Action] - SongSelected(song=Song(id=13, title=Watermelon Sugar, artistDisplayName=Harry Styles, releaseDate=2025-12-18))
SongsViewModel@021ba2c6: [Effect] - OpenSongDetails(songId=13)
SongsViewModel@021ba2c6: [Lifecycle] - onUnsubscribe
SongDetailViewModel@07598509: [Initial State] - SongDetailState(song=null, error=null)
SongDetailViewModel@07598509: [Action] - Init(songId=13)
SongDetailViewModel@07598509: [State] - SongDetailState(song=Song(id=13, title=Watermelon Sugar, artistDisplayName=Harry Styles, releaseDate=2025-12-18), error=null)
SongDetailViewModel@07598509: [Lifecycle] - onInit
SongDetailViewModel@07598509: [Lifecycle] - onSubscribe
```

## License

```
Copyright 2025 Marcin Adamczewski

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
