package dev.spaceapps.weather.feature.cities.domain

import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.domain.Outcome
import dev.spaceapps.weather.feature.cities.domain.model.CityCandidate
import dev.spaceapps.weather.feature.cities.domain.model.CitySearchResult
import dev.spaceapps.weather.feature.cities.domain.model.SavedCity
import kotlinx.coroutines.flow.Flow

interface SavedCitiesRepository {
    fun observeCities(): Flow<List<SavedCity>>

    suspend fun getCity(id: Long): SavedCity?

    /** Saves [candidate] at the end of the list. Adding an already saved place returns the existing city. */
    suspend fun add(candidate: CityCandidate): SavedCity

    /** Removes the city and its cached weather. Returns the removed city (for undo), or null. */
    suspend fun remove(id: Long): SavedCity?

    /** Undo for [remove]: puts the city back with its original id and position. */
    suspend fun restore(city: SavedCity)
}

interface CitySearchRepository {
    /** Up to 5 matches, each marked when it is already saved. A blank query returns no results. */
    suspend fun search(query: String): Outcome<List<CitySearchResult>, DataError.Remote>
}
