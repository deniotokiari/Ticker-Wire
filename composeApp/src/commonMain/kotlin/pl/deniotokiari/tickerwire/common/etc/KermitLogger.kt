package pl.deniotokiari.tickerwire.common.etc

import org.koin.core.annotation.Single

@Single(binds = [Logger::class])
class KermitLogger : Logger {
    private val kermit = co.touchlab.kermit.Logger

    override fun d(tag: String, message: String) {
        kermit.d(tag, null, { message })
    }

    override fun i(tag: String, message: String) {
        kermit.i(tag, null, { message })
    }

    override fun e(tag: String, message: String) {
        kermit.e(tag, null, { message })
    }
}
