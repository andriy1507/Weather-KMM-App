package dev.spaceapps.weather.feature.cities.presentation.detail

import dev.spaceapps.weather.feature.weather.presentation.CurrentWeatherUi
import dev.spaceapps.weather.feature.weather.presentation.WeatherError

data class CityWeatherState(
    val title: String = "",
    val subtitle: String = "",
    val weather: CurrentWeatherUi? = null,
    val isFetching: Boolean = false,
    /** Shown in place of content when there is no cached weather to display. */
    val error: WeatherError? = null,
    val showDeleteConfirmation: Boolean = false,
) {
    val isLoading: Boolean get() = isFetching && weather == null
    val isRefreshing: Boolean get() = isFetching && weather != null
}

sealed interface CityWeatherIntent {
    /** Screen became visible: refreshes if the cached weather is stale. */
    data object ScreenStarted : CityWeatherIntent
    data object Refresh : CityWeatherIntent
    data object DeleteClicked : CityWeatherIntent
    data object DeleteConfirmed : CityWeatherIntent
    data object DeleteDismissed : CityWeatherIntent
}

sealed interface CityWeatherEffect {
    /** Leave the screen: the city was deleted (here or elsewhere). */
    data object Close : CityWeatherEffect
    /** Transient error while cached weather stays visible. */
    data class ShowError(val error: WeatherError) : CityWeatherEffect
}
