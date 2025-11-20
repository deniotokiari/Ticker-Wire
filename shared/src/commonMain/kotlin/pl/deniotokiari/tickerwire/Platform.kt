package pl.deniotokiari.tickerwire

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform