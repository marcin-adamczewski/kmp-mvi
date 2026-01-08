# kmp-mvi

A lightweight, flexible, and powerful MVI (Model-View-Intent) library for Kotlin Multiplatform. Designed to simplify state management in your KMP projects with first-class support for Coroutines, Flow, and Compose.

## Features

- **Multiplatform**: Works on Android, iOS, JVM, Wasm, and Linux.
- **Side Effects**: Robust handling of one-time events (effects) like navigation or toast messages.
- **Powerful UI actions handling**: Handle UI actions with power of Flow and Coroutines.
- **ViewModel**: Optional integration with `androidx.lifecycle.ViewModel`.
- **Progress Management**: Easy-to-use API for tracking loading states across multiple operations.
- **Error Management**: Centralized error handling and propagation to the UI.
- **Logging**: Built-in support for logging state transitions, actions, and effects.
- **Lifecycle support**: Observe lifecycle events and react accordingly.
- **Compose Support**: Dedicated extensions for state collection and effect handling in Jetpack and Multiplatform Compose.
- **Test utils**: Helper functions for testing your MVI components with Turbine.

## Installation

Add the following to your `build.gradle.kts` in your KMP project:

```kotlin
repositories {
    mavenCentral()
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Core MVI components - if ViewModel integration is not required
            implementation("com.adamczewski.kmpmvi:core:libVersion")
            
            // Core components + ViewModel integration
            implementation("com.adamczewski.kmpmvi:viewmodel:libVersion")
            
            // Compose Multiplatform extensions
            implementation("com.adamczewski.kmpmvi:compose:libVersion")

            // Test utils
            implementation("com.adamczewski.kmpmvi:test:libVersion")
        }
    }
}
```

## Usage

### 1. Define your MVI Components

Define your state, actions, and effects. They should implement `MviState`, `MviAction`, and `MviEffect` respectively.

```kotlin
// State of your UI.
data class SongsState(
    val isLoading: Boolean = false,
    val error: UiError? = null,
    val songs: List<Song> = emptyList(),
) : MviState

// Actions that can be dispatched from the UI.
sealed interface SongsAction : MviAction {
    data class SearchQueryChanged(val query: String) : SongsAction
    data class SongSelected(val song: Song) : SongsAction
}

// Side effects that are emitted from the MviContainer and observed by the UI.
// Usually those are navigation events, toast messages, etc.
sealed interface SongsEffect : MviEffect {
    data class OpenSongDetails(val songId: String) : SongsEffect
    data class OpenMediaPlayer(val songId: String) : SongsEffect
}
```

### 2. Create your ViewModel or MviStateManager

Extend `MviViewModel` or `MviStateManager` and implement `handleActions()`.

```kotlin
class SongsViewModel(
    private val repository: MusicRepository,
    private val errorManager: ErrorManager,
) : MviViewModel<SongsAction, SongsState, SongsEffect>(
    initialState = SongsState()
) {

    init {
        // onInit is called once, when the first subscriber connects to the state.
        // Use it for initilization or create an Init action and dispatch it manually whenever you want.
        onInit { 
            // withProgress - Shows progress at the beggining of the block and hides it when completed 
            withProgress {
                repository.fetchSongs()
                    // setState - Updates state based on the current state
                    .onSuccess { setState { copy(songs = it, error = null) } }
                    .onError { errorManager.addError(it.toLongError()) }
            }
        }

        // Optional - Handle all errors in one place using ErrorManager
        observeError(errorManager) { error ->
            setState { copy(error = error) }
        }

        // Optional - Handle all loading events in one place using ProgressManager
        observeProgress { isLoading ->
            setState { copy(isLoading = isLoading) }
        }
    }

    override fun ActionsManager<SongsAction>.handleActions() {
        // Instead of using onInit block, you can dispatch Init action manually whenever you want.
        onActionSingle<Init> {
            // Initialization logic here
        }
        
        // When song was selected in UI, emit navigation effect OpenSongDetails
        onAction<SongSelected> {
            analytics.trackSongSelected(it.song.id)
            setEffect { OpenSongDetails(it.song.id) }
        }
        
        // onActionFlow is very powerful and allows to act on UI actions using Flow transformers.
        // In this example we debounce every search text change so the search doesn't run on every keystroke.
        // This flow is automatically collected.
        onActionFlow<SearchQueryChanged> { action ->
            debounce(300)
                .distinctUntilChanged()
                .map { repository.searchSongs(action.query) }
                .onSuccess { setState { copy(songs = it, error = null) } }
                .onError { errorManager.addError(it.toLongError()) }
        }
    }
}
```

> **Note**: The library features a built-in lifecycle management system based on the number of active subscribers to the state flow. You can react to these changes using `onInit`, `onSubscribe`, and `onUnsubscribe` callbacks.
> The lifecycle of the MVI component is automatically managed. When using `collectAsStateWithLifecycle()` in Compose, it will trigger `onSubscribe` when the screen enters the foreground and `onUnsubscribe` when it leaves, allowing for efficient resource management.

### 3. Use in Compose

Connect your UI with the ViewModel using the provided extensions.

```kotlin
@Composable
fun SongsScreen(viewModel: SongsViewModel) {
    // Collect state with lifecycle awareness
    val state by viewModel.collectAsStateWithLifecycle()
    var searchQuery by rememberSavable { mutableStateOf("") }

    // Handle one-time side effects
    viewModel.handleEffects { effect ->
        when (effect) {
            is SongsEffect.OpenSongDetails -> { /* navigate to details */ }
        }
    }

    Column {
        TextField(
            value = searchQuery,
            onValueChange = { 
                searchQuery = it
                // Send search query to the ViewModel
                viewModel.submitAction(SongsAction.SearchQueryChanged(it)) 
            }
        )
        
        state.songs.forEach { song ->
            SongItem(
                text = song.title,
                onClick = {
                    // Send song click event to the ViewModel
                    viewModel.submitAction(SongsAction.SongSelected(song)) 
                }
            )
        }
    }
}
```

You can pass viewmodel::submitAction function down the hierarchy to your child components. 
That way you don't have to pass many event functions down the hierarchy.

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
onAction<AddToFavoritesClicked> {
    // Automatically manage loading state during the block
    withProgress {
        val songs = repository.getSongs()
        setState { copy(songs = songs) }
    }
}

// Or using Flow transformers
onActionFlow<Init> {
    repository.getSongsFlow()
        .watchProgress() // Shows loading on start and hides when first value is received or Flow is completed
        .onSuccess { songs ->
            setState { copy(songs = songs) }
        }
    }
}
```

### Logging

Built-in support for logging to track all actions, state changes, effects, and lifecycle events in your console. This is extremely helpful for debugging complex state transitions and verifying behavior in both code and tests.
You can also send logs to a remote service, like Crashlytics so it's much easier to understand why something crashed.

Example log output:

```text
SongsViewModel@021ba2c6: [Initial State] - SongsState(isLoading=true, error=null, songs=null)
SongsViewModel@021ba2c6: [Lifecycle] - onInit
SongsViewModel@021ba2c6: [Lifecycle] - onSubscribe
SongsViewModel@021ba2c6: [State] - SongsState(isLoading=false, error=null, songs=[Song(id=1, title=Midnight City, artistDisplayName=M83, releaseDate=2025-12-18)])
SongsViewModel@021ba2c6: [Action] - SearchQueryChanged(query=Water)
SongsViewModel@021ba2c6: [Action] - SongSelected(song=Song(id=13, title=Watermelon Sugar, artistDisplayName=Harry Styles, releaseDate=2025-12-18))
SongsViewModel@021ba2c6: [Effect] - OpenSongDetails(songId=13)
SongsViewModel@021ba2c6: [Lifecycle] - onUnsubscribe
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
