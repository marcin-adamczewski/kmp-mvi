package com.adamczewski.kmpmvi.sample

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.adamczewski.kmpmvi.mvi.MviConfig
import com.adamczewski.kmpmvi.mvi.settings.buildMviSettingsProvider
import com.adamczewski.kmpmvi.sample.screens.InitScreen
import com.adamczewski.kmpmvi.sample.screens.detail.SongDetailsScreen
import com.adamczewski.kmpmvi.sample.screens.dsl.list.SongsDslScreen
import com.adamczewski.kmpmvi.sample.screens.list.SongsScreen
import kotlinx.serialization.Serializable

@Serializable
data object InitScreenDestination

@Serializable
object SongsDestination

@Serializable
data class SongDetailsDestination(val songId: String)

@Composable
fun App() {
    MviConfig.apply {
        // Customise default settings.
        settingsProvider = buildMviSettingsProvider { tag, klass ->
            // In a production app it could be something like
            // isLoggerEnabled = BuildConfigUtils.isDebugBuild()
            isLoggerEnabled = true
            effectsBufferSize = 20
        }
    }

    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        var isDslSelected by remember { mutableStateOf(false) }

        Surface {
            val navController: NavHostController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = InitScreenDestination
            ) {
                composable<InitScreenDestination> {
                    InitScreen(
                        openDslStyle = {
                            isDslSelected = true
                            navController.navigate(SongsDestination)
                        },
                        openIneritanceStyle = {
                            isDslSelected = false
                            navController.navigate(SongsDestination)
                        }
                    )
                }

                composable<SongsDestination> {
                    if (isDslSelected) {
                        SongsDslScreen(navigateToDetails = { songId ->
                            navController.navigate(SongDetailsDestination(songId))
                        })
                    } else {
                        SongsScreen(navigateToDetails = { songId ->
                            navController.navigate(SongDetailsDestination(songId))
                        })
                    }

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
