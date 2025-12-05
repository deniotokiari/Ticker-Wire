package pl.deniotokiari.tickerwire.common.analytics

import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class AnalyticsModule {
    @Single
    fun provideFirebaseAnalyticsBridge(): FirebaseAnalyticsBridge = FirebaseAnalyticsBridge()
}
