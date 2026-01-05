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
import com.adamczewski.kmpmvi.mvi.logger.DefaultMviLogger
import com.adamczewski.kmpmvi.mvi.settings.DefaultMviSettingsProvider
import com.adamczewski.kmpmvi.mvi.settings.MviSettingProviderBuilder
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
        // Customise default settings. You can use DefaultMviSettingsProvider and override
        // any default property passing it to the .copy() function.
        settingsProvider = MviSettingProviderBuilder.withDefaultSettings { settings, logTag, klass ->
            settings.copy(
                // In a production app it could be something like
                // isLoggerEnabled = BuildConfigUtils.isDebugBuild()
                isLoggerEnabled = true,
                effectsBufferSize = 20
            )
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
