package dev.spaceapps.weather.feature.cities.presentation

import dev.spaceapps.weather.feature.cities.domain.model.CityCandidate
import dev.spaceapps.weather.feature.cities.domain.model.SavedCity

/** Second line under a city name: "Illinois, US" or just "UA". */
internal fun citySubtitle(state: String?, countryCode: String): String =
    listOfNotNull(state, countryCode).joinToString(", ")

internal val SavedCity.subtitle: String get() = citySubtitle(state, countryCode)

internal val CityCandidate.subtitle: String get() = citySubtitle(state, countryCode)
