package dev.spaceapps.weather.feature.currentweather.presentation

import dev.spaceapps.weather.core.settings.ThemeMode
import dev.spaceapps.weather.core.settings.UnitSystem

data class CurrentWeatherState(
    val weather: CurrentWeatherUi? = null,
    val isFetching: Boolean = false,
    val permission: LocationPermission = LocationPermission.UNKNOWN,
    /** Shown in place of content when there is no cached weather to display. */
    val error: CurrentWeatherError? = null,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    /** First load: nothing to show yet. */
    val isLoading: Boolean get() = isFetching && weather == null

    /** Updating while cached weather is visible (pull-to-refresh indicator). */
    val isRefreshing: Boolean get() = isFetching && weather != null
}

enum class LocationPermission { UNKNOWN, GRANTED, DENIED }

/** Platform UIs map these to localized messages. */
enum class CurrentWeatherError {
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

sealed interface CurrentWeatherIntent {
    /** Screen became visible. Safe to send repeatedly; initial work runs once. */
    data object ScreenStarted : CurrentWeatherIntent
    data class PermissionResult(val granted: Boolean) : CurrentWeatherIntent
    data object GrantPermissionClicked : CurrentWeatherIntent
    data object Refresh : CurrentWeatherIntent
    data class ChangeUnitSystem(val unitSystem: UnitSystem) : CurrentWeatherIntent
    data class ChangeThemeMode(val themeMode: ThemeMode) : CurrentWeatherIntent
}

sealed interface CurrentWeatherEffect {
    /**
     * Ask for location access. The platform decides how: the system dialog, or the app's
     * system settings page when the user has permanently denied it.
     */
    data object RequestLocationPermission : CurrentWeatherEffect

    /** Transient error while cached weather stays visible (e.g. a snackbar). */
    data class ShowError(val error: CurrentWeatherError) : CurrentWeatherEffect
}
