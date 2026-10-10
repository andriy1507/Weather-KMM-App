package dev.spaceapps.weather.feature.currentweather.presentation

import dev.spaceapps.weather.feature.weather.presentation.CurrentWeatherUi
import dev.spaceapps.weather.feature.weather.presentation.WeatherError

data class CurrentWeatherState(
    val weather: CurrentWeatherUi? = null,
    val isFetching: Boolean = false,
    val permission: LocationPermission = LocationPermission.UNKNOWN,
    /** Shown in place of content when there is no cached weather to display. */
    val error: WeatherError? = null,
) {
    /** First load: nothing to show yet. */
    val isLoading: Boolean get() = isFetching && weather == null

    /** Updating while cached weather is visible (pull-to-refresh indicator). */
    val isRefreshing: Boolean get() = isFetching && weather != null
}

enum class LocationPermission { UNKNOWN, GRANTED, DENIED }

sealed interface CurrentWeatherIntent {
    /** Screen became visible. Safe to send repeatedly; initial work runs once. */
    data object ScreenStarted : CurrentWeatherIntent
    data class PermissionResult(val granted: Boolean) : CurrentWeatherIntent
    data object GrantPermissionClicked : CurrentWeatherIntent
    data object Refresh : CurrentWeatherIntent
}

sealed interface CurrentWeatherEffect {
    /**
     * Ask for location access. The platform decides how: the system dialog, or the app's
     * system settings page when the user has permanently denied it.
     */
    data object RequestLocationPermission : CurrentWeatherEffect

    /** Transient error while cached weather stays visible (e.g. a snackbar). */
    data class ShowError(val error: WeatherError) : CurrentWeatherEffect
}
