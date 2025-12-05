package pl.deniotokiari.tickerwire.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import pl.deniotokiari.tickerwire.di.appModule
import pl.deniotokiari.tickerwire.di.cacheModule
import pl.deniotokiari.tickerwire.di.providerModule

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(
            appModule,
            cacheModule,
            providerModule,
        )
    }
}
