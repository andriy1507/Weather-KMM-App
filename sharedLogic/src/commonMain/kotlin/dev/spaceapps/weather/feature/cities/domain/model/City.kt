package dev.spaceapps.weather.feature.cities.domain.model

import dev.spaceapps.weather.core.location.Coordinates

/** A place returned by city search that can be saved. */
data class CityCandidate(
    val name: String,
    /** State/region, when the geocoder returns one (e.g. US states). */
    val state: String?,
    val countryCode: String,
    val coordinates: Coordinates,
)

data class CitySearchResult(
    val city: CityCandidate,
    val isSaved: Boolean,
)

data class SavedCity(
    val id: Long,
    val name: String,
    val state: String?,
    val countryCode: String,
    val coordinates: Coordinates,
    /** Display order; preserved when a deletion is undone. */
    val position: Int,
    val addedAtMillis: Long,
)
