package pl.deniotokiari.tickerwire.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

fun <T> NavController.popBackStackWithData(key: String, data: T, serializer: KSerializer<T>) {
    val json = Json.encodeToString(serializer = serializer, data)

    previousBackStackEntry?.savedStateHandle?.set(key, json)
    popBackStack()
}

@Composable
fun <T> CollectData(
    key: String,
    serializer: KSerializer<T>,
    navController: NavController,
    onData: (T) -> Unit,
) {
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle

    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow<String?>(key, null)?.collect { item ->
            item?.let {
                val data = Json.decodeFromString(deserializer = serializer, item)

                onData(data)

                savedStateHandle[key] = null
            }
        }
    }
}
