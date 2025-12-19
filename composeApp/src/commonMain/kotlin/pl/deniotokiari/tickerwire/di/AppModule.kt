package pl.deniotokiari.tickerwire.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module


@KoinApplication(modules = [AppModule::class])
object AppKoinApplication

@Module
@ComponentScan(
    "pl.deniotokiari.tickerwire.common",
    "pl.deniotokiari.tickerwire.feature"
)
class AppModule
