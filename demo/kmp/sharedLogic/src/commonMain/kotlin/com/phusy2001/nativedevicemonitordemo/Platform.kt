package com.phusy2001.nativedevicemonitordemo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform