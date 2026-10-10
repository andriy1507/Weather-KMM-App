package dev.spaceapps.weather.ui

import dev.spaceapps.weather.feature.weather.presentation.WeatherError
import org.jetbrains.compose.resources.StringResource
import weatherapp.sharedui.generated.resources.Res
import weatherapp.sharedui.generated.resources.error_invalid_api_key
import weatherapp.sharedui.generated.resources.error_location_disabled
import weatherapp.sharedui.generated.resources.error_location_permission
import weatherapp.sharedui.generated.resources.error_location_unavailable
import weatherapp.sharedui.generated.resources.error_no_internet
import weatherapp.sharedui.generated.resources.error_rate_limited
import weatherapp.sharedui.generated.resources.error_server
import weatherapp.sharedui.generated.resources.error_timeout
import weatherapp.sharedui.generated.resources.error_unknown

internal val WeatherError.message: StringResource
    get() = when (this) {
        WeatherError.NO_INTERNET -> Res.string.error_no_internet
        WeatherError.TIMEOUT -> Res.string.error_timeout
        WeatherError.INVALID_API_KEY -> Res.string.error_invalid_api_key
        WeatherError.RATE_LIMITED -> Res.string.error_rate_limited
        WeatherError.SERVER -> Res.string.error_server
        WeatherError.LOCATION_PERMISSION_DENIED -> Res.string.error_location_permission
        WeatherError.LOCATION_DISABLED -> Res.string.error_location_disabled
        WeatherError.LOCATION_UNAVAILABLE -> Res.string.error_location_unavailable
        WeatherError.UNKNOWN -> Res.string.error_unknown
    }
