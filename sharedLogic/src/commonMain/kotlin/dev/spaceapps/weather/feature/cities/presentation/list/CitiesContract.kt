package dev.spaceapps.weather.feature.cities.presentation.list

import dev.spaceapps.weather.feature.weather.presentation.WeatherError

data class CitiesState(
    val cities: List<CityRowUi> = emptyList(),
    /** False until saved cities were read once (avoids flashing the empty state). */
    val isLoaded: Boolean = false,
    /** Pull-to-refresh in progress (automatic refreshes of stale cities are silent). */
    val isRefreshing: Boolean = false,
) {
    val isEmpty: Boolean get() = isLoaded && cities.isEmpty()
}

/** One list row. Weather fields are null until the city's weather has been fetched once. */
data class CityRowUi(
    val id: Long,
    val name: String,
    val subtitle: String,
    val temperature: String?,
    val conditionText: String?,
    val iconUrl: String?,
)

sealed interface CitiesIntent {
    /** List became visible: refreshes cities whose weather is stale. Safe to send repeatedly. */
    data object ScreenStarted : CitiesIntent
    /** Pull-to-refresh: refreshes every city. */
    data object Refresh : CitiesIntent
    data class CityClicked(val cityId: Long) : CitiesIntent
    data object AddClicked : CitiesIntent
    data class DeleteCity(val cityId: Long) : CitiesIntent
    data object UndoDelete : CitiesIntent
}

sealed interface CitiesEffect {
    data class OpenCity(val cityId: Long) : CitiesEffect
    data object OpenSearch : CitiesEffect
    /** Offer to undo the deletion (e.g. a snackbar action). */
    data class ShowUndo(val cityName: String) : CitiesEffect
    data class ShowError(val error: WeatherError) : CitiesEffect
}
