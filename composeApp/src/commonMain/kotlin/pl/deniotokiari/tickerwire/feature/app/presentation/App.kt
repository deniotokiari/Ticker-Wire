package pl.deniotokiari.tickerwire.feature.app.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.ksp.generated.startKoin
import pl.deniotokiari.tickerwire.di.AppKoinApplication
import pl.deniotokiari.tickerwire.feature.home.presentation.HomeScreen
import pl.deniotokiari.tickerwire.feature.search.presentation.SearchScreen
import pl.deniotokiari.tickerwire.navigation.Route
import pl.deniotokiari.tickerwire.theme.AppTheme

@Composable
@Preview
fun App() {
    KoinApplication(application = { AppKoinApplication.startKoin() }) {
        val viewModel: AppViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsState()

        AppTheme(darkTheme = uiState.isDarkTheme) {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = Route.Home,
            ) {
                composable<Route.Home> { HomeScreen(navController) }
                composable<Route.Search> { SearchScreen(navController) }
            }
        }
    }
}
