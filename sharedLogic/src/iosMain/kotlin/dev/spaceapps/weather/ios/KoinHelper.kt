package dev.spaceapps.weather.ios

import dev.spaceapps.weather.core.di.initKoin

/** Entry point for Swift: `KoinHelper.shared.start()` from `iOSApp.init`. */
object KoinHelper {
    fun start() {
        initKoin()
    }
}
