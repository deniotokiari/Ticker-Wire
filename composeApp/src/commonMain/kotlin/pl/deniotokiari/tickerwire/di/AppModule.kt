package pl.deniotokiari.tickerwire.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import pl.deniotokiari.tickerwire.common.analytics.AnalyticsModule


@KoinApplication(modules = [AppModule::class])
object AppKoinApplication

@Module(includes = [AnalyticsModule::class])
@ComponentScan(
    "pl.deniotokiari.tickerwire.common",
    "pl.deniotokiari.tickerwire.feature"
)
class AppModule
