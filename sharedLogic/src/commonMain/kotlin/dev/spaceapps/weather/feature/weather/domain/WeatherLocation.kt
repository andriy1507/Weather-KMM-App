package dev.spaceapps.weather.feature.weather.domain

/** A place whose weather is cached: the device location or one of the saved cities. */
sealed interface WeatherLocation {
    /** Key of the cached weather row. */
    val cacheKey: String

    data object Current : WeatherLocation {
        override val cacheKey: String = "current"
    }

    data class SavedCity(val cityId: Long) : WeatherLocation {
        override val cacheKey: String = "city:$cityId"
    }
}
