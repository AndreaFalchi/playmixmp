package com.falchi.playmixmp

interface Platform {
    val name: String
    val version: String
}

expect fun getPlatform(): Platform