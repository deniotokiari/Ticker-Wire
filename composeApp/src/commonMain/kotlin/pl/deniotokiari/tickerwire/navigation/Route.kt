package pl.deniotokiari.tickerwire.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Home : Route

    @Serializable
    data object Search : Route

    companion object {
        const val KEY_SEARCH_ITEM = "SEARCH_ITEM"
    }
}
