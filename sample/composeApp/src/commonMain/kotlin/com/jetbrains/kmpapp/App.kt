package com.jetbrains.kmpapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.adamczewski.kmpmvi.mvi.MviConfig
import com.adamczewski.kmpmvi.mvi.settings.DefaultMviSettingsProvider
import com.adamczewski.kmpmvi.mvi.settings.MviSettingsProvider
import com.jetbrains.kmpapp.screens.detail.SongDetailsScreen
import com.jetbrains.kmpapp.screens.list.SongsScreen
import kotlinx.serialization.Serializable

@Serializable
object SongsDestination

@Serializable
data class SongDetailsDestination(val songId: String)

@Composable
fun App() {
    MviConfig.apply {
        // In a production app it could be something like
        // canLog = BuildConfigUtils.isDebugBuild()
        canLog = true
        // Customise default settings. You can use DefaultMviSettingsProvider and override
        // any default property passing it to the .copy() function.
        settingsProvider = MviSettingsProvider { tag, klass ->
            val defaultSettings = DefaultMviSettingsProvider.provide(tag, klass)
            defaultSettings.copy(effectsBufferSize = 20)
        }
    }

    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        Surface {
            val navController: NavHostController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = SongsDestination
            ) {
                composable<SongsDestination> {
                    SongsScreen(navigateToDetails = { songId ->
                        navController.navigate(SongDetailsDestination(songId))
                    })
                }
                composable<SongDetailsDestination> { backStackEntry ->
                    SongDetailsScreen(
                        songId = backStackEntry.toRoute<SongDetailsDestination>().songId,
                        navigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
