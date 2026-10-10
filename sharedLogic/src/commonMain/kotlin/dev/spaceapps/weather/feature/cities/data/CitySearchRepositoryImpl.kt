package dev.spaceapps.weather.feature.cities.data

import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.domain.Outcome
import dev.spaceapps.weather.core.domain.map
import dev.spaceapps.weather.core.location.Coordinates
import dev.spaceapps.weather.core.network.safeApiCall
import dev.spaceapps.weather.feature.cities.data.local.SavedCityDao
import dev.spaceapps.weather.feature.cities.data.remote.GeocodingApi
import dev.spaceapps.weather.feature.cities.domain.CitySearchRepository
import dev.spaceapps.weather.feature.cities.domain.model.CitySearchResult

class CitySearchRepositoryImpl(
    private val api: GeocodingApi,
    private val dao: SavedCityDao,
) : CitySearchRepository {

    override suspend fun search(query: String): Outcome<List<CitySearchResult>, DataError.Remote> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return Outcome.Success(emptyList())

        return safeApiCall { api.searchCities(trimmed, SEARCH_LIMIT) }.map { results ->
            val savedCoordinates = dao.getAll().mapTo(HashSet()) { Coordinates(it.latitude, it.longitude) }
            results
                .map { it.toCandidate() }
                // The geocoder sometimes returns the same place twice (e.g. under different local names).
                .distinctBy { it.coordinates }
                .map { CitySearchResult(city = it, isSaved = it.coordinates in savedCoordinates) }
        }
    }

    private companion object {
        const val SEARCH_LIMIT = 5
    }
}
