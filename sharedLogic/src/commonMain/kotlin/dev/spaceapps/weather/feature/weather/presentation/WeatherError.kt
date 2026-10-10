package dev.spaceapps.weather.feature.weather.presentation

import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.location.LocationError

/** Errors shown by any screen. Platform UIs map these to localized messages. */
enum class WeatherError {
    NO_INTERNET,
    TIMEOUT,
    INVALID_API_KEY,
    RATE_LIMITED,
    SERVER,
    LOCATION_PERMISSION_DENIED,
    LOCATION_DISABLED,
    LOCATION_UNAVAILABLE,
    UNKNOWN,
}

fun DataError.Remote.toWeatherError(): WeatherError = when (this) {
    DataError.Remote.NO_INTERNET -> WeatherError.NO_INTERNET
    DataError.Remote.REQUEST_TIMEOUT -> WeatherError.TIMEOUT
    DataError.Remote.UNAUTHORIZED -> WeatherError.INVALID_API_KEY
    DataError.Remote.TOO_MANY_REQUESTS -> WeatherError.RATE_LIMITED
    DataError.Remote.SERVER -> WeatherError.SERVER
    DataError.Remote.SERIALIZATION, DataError.Remote.UNKNOWN -> WeatherError.UNKNOWN
}

fun LocationError.toWeatherError(): WeatherError = when (this) {
    LocationError.PERMISSION_DENIED -> WeatherError.LOCATION_PERMISSION_DENIED
    LocationError.LOCATION_DISABLED -> WeatherError.LOCATION_DISABLED
    LocationError.UNAVAILABLE -> WeatherError.LOCATION_UNAVAILABLE
}
