package pl.deniotokiari.tickerwire.feature.app.presentation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.ksp.generated.startKoin
import pl.deniotokiari.tickerwire.di.AppKoinApplication
import pl.deniotokiari.tickerwire.feature.home.presentation.HomeScreen
import pl.deniotokiari.tickerwire.feature.search.presentation.SearchScreen
import pl.deniotokiari.tickerwire.navigation.Route
import pl.deniotokiari.tickerwire.theme.AppTheme

@Suppress("ModifierRequired")
@Composable
fun App() {
    KoinApplication(application = { AppKoinApplication.startKoin() }) {
        AppScreen()
    }
}

@Composable
private fun AppScreen(
    viewModel: AppViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AppTheme(darkTheme = uiState.isDarkTheme) {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = Route.Home,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
        ) {
            composable<Route.Home> { HomeScreen(navController) }
            composable<Route.Search> { SearchScreen(navController) }
        }
    }
}
