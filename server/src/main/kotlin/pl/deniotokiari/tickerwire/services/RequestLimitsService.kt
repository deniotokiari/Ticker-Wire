package pl.deniotokiari.tickerwire.services

import kotlinx.serialization.serializer
import pl.deniotokiari.tickerwire.models.RequestLimits

private const val LIMITS = "limits"

class RequestLimitsService(
    private val firebaseRemoteConfigService: FirebaseRemoteConfigService,
) {
    private var _limits = RequestLimits(
        news = 5,
        tickers = 50,
    )
    val limits: RequestLimits get() = _limits

    init {
        refresh()
    }

    fun refresh() {
        val result = firebaseRemoteConfigService.get(
            key = LIMITS,
            kSerializer = serializer<RequestLimits>(),
        ) ?: return

        _limits = result
    }
}
