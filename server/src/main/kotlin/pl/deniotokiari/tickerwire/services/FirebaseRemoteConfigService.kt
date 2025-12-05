package pl.deniotokiari.tickerwire.services

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ParameterValue
import com.google.firebase.remoteconfig.ParameterValueType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class FirebaseRemoteConfigService {
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowStructuredMapKeys = true
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, kSerializer: KSerializer<T>): T? {
        val parameter = remoteConfig.template.parameters[key] ?: return null
        val parameterValue = parameter.defaultValue as? ParameterValue.Explicit ?: return null

        return when (parameter.valueType) {
            ParameterValueType.STRING -> parameterValue.value
            ParameterValueType.BOOLEAN -> parameterValue.value.toBoolean()
            ParameterValueType.NUMBER -> parameterValue.value.toInt()
            ParameterValueType.JSON -> json.decodeFromString(
                deserializer = kSerializer,
                string = parameterValue.value,
            )
        } as? T
    }

    fun <T> set(key: String, value: T, kSerializer: KSerializer<T>) {
        val template = remoteConfig.template
        val parameter = template.parameters[key] ?: return

        runCatching {
            when (parameter.valueType) {
                ParameterValueType.STRING -> value as String
                ParameterValueType.BOOLEAN -> value.toString()
                ParameterValueType.NUMBER -> value.toString()
                ParameterValueType.JSON -> json.encodeToString(kSerializer, value)
            }
        }.onSuccess { value ->
            parameter.setDefaultValue(ParameterValue.Explicit.of(value))
            remoteConfig.forcePublishTemplate(template)
        }
    }

    fun <T> setAsync(key: String, value: T, kSerializer: KSerializer<T>) {
        val template = remoteConfig.template
        val parameter = template.parameters[key] ?: return

        runCatching {
            when (parameter.valueType) {
                ParameterValueType.STRING -> value as String
                ParameterValueType.BOOLEAN -> value.toString()
                ParameterValueType.NUMBER -> value.toString()
                ParameterValueType.JSON -> json.encodeToString(kSerializer, value)
            }
        }.onSuccess { value ->
            parameter.setDefaultValue(ParameterValue.Explicit.of(value))
            remoteConfig.forcePublishTemplateAsync(template)
        }
    }
}
