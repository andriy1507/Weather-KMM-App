package dev.spaceapps.weather.feature.cities.presentation.search

import dev.spaceapps.weather.feature.weather.presentation.WeatherError

data class CitySearchState(
    val query: String = "",
    val results: List<CitySearchResultUi> = emptyList(),
    val isSearching: Boolean = false,
    val error: WeatherError? = null,
    /** A search for the current query finished with no matches. */
    val showNoResults: Boolean = false,
)

data class CitySearchResultUi(
    /** Stable key of the result within the current list. */
    val id: String,
    val name: String,
    val subtitle: String,
    val isSaved: Boolean,
)

sealed interface CitySearchIntent {
    data class QueryChanged(val query: String) : CitySearchIntent
    data class ResultClicked(val resultId: String) : CitySearchIntent
    data object Retry : CitySearchIntent
}

sealed interface CitySearchEffect {
    /** The city is saved (or was already); close search. */
    data class CityAdded(val cityId: Long) : CitySearchEffect
}
