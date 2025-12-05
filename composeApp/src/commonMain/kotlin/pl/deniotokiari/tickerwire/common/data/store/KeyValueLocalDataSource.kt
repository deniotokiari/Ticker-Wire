package pl.deniotokiari.tickerwire.common.data.store

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.serialization.decodeValueOrNull
import com.russhwolf.settings.serialization.encodeValue
import com.russhwolf.settings.serialization.removeValue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer

class KeyValueLocalDataSource(
    private val name: String,
    private val settings: Settings = Settings(),
) {
    fun setBoolean(key: String, value: Boolean) {
        settings.putBoolean(getKey(key), value)
    }

    fun getBoolean(key: String): Boolean? = settings.getBooleanOrNull(getKey(key))

    fun setString(key: String, value: String) {
        settings.putString(getKey(key), value)
    }

    fun getString(key: String): String? = settings.getStringOrNull(getKey(key))

    @OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class)
    fun <T> getValue(key: String, kSerializer: KSerializer<T>): T? {
        return settings.decodeValueOrNull(
            serializer = kSerializer,
            key = getKey(key),
        )
    }

    @OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class)
    fun <T> setValue(key: String, value: T, kSerializer: KSerializer<T>) {
        settings.encodeValue(
            serializer = kSerializer,
            key = getKey(key),
            value = value,
        )
    }

    @OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class)
    fun <T> removeValue(key: String, kSerializer: KSerializer<T>) {
        settings.removeValue(
            serializer = kSerializer,
            key = getKey(key),
        )
    }

    fun remove(key: String) {
        settings.remove(getKey(key))
    }


    fun clear() {
        settings.keys.forEach { key ->
            if (key.startsWith(name)) {
                settings.remove(key)
            }
        }
    }

    fun keys(): Set<String> = settings.keys

    private fun getKey(key: String): String = "${name}_$key"
}
