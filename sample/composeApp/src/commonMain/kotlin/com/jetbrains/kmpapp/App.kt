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
import com.jetbrains.kmpapp.screens.detail.DetailScreen
import com.jetbrains.kmpapp.screens.list.ListScreen
import kotlinx.serialization.Serializable

@Serializable
object ListDestination

@Serializable
data class DetailDestination(val songId: String)

@Composable
fun App() {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        Surface {
            val navController: NavHostController = rememberNavController()
            NavHost(navController = navController, startDestination = ListDestination) {
                composable<ListDestination> {
                    ListScreen(navigateToDetails = { songId ->
                        navController.navigate(DetailDestination(songId))
                    })
                }
                composable<DetailDestination> { backStackEntry ->
                    DetailScreen(
                        songId = backStackEntry.toRoute<DetailDestination>().songId,
                        navigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
