package dev.ajvanegasv.kontio

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform