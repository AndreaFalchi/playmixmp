package com.falchi.playmixmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform