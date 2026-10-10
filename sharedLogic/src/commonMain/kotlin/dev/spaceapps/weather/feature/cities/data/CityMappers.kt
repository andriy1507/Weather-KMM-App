package dev.spaceapps.weather.feature.cities.data

import dev.spaceapps.weather.core.location.Coordinates
import dev.spaceapps.weather.feature.cities.data.local.SavedCityEntity
import dev.spaceapps.weather.feature.cities.data.remote.GeocodingResultDto
import dev.spaceapps.weather.feature.cities.domain.model.CityCandidate
import dev.spaceapps.weather.feature.cities.domain.model.SavedCity
import kotlin.math.roundToLong

private const val COORDINATE_SCALE = 10_000.0 // 4 decimals ≈ 11 m

/**
 * Coordinates are rounded so the same place from different searches compares equal
 * (and the unique index on `saved_city` can reject duplicates).
 */
internal fun Coordinates.rounded(): Coordinates = Coordinates(
    latitude = (latitude * COORDINATE_SCALE).roundToLong() / COORDINATE_SCALE,
    longitude = (longitude * COORDINATE_SCALE).roundToLong() / COORDINATE_SCALE,
)

internal fun GeocodingResultDto.toCandidate(): CityCandidate = CityCandidate(
    name = name,
    state = state?.takeIf { it.isNotBlank() },
    countryCode = country,
    coordinates = Coordinates(lat, lon).rounded(),
)

internal fun SavedCityEntity.toDomain(): SavedCity = SavedCity(
    id = id,
    name = name,
    state = state,
    countryCode = countryCode,
    coordinates = Coordinates(latitude, longitude),
    position = position,
    addedAtMillis = addedAtMillis,
)

internal fun SavedCity.toEntity(): SavedCityEntity = SavedCityEntity(
    id = id,
    name = name,
    state = state,
    countryCode = countryCode,
    latitude = coordinates.latitude,
    longitude = coordinates.longitude,
    position = position,
    addedAtMillis = addedAtMillis,
)

internal fun CityCandidate.toNewEntity(addedAtMillis: Long): SavedCityEntity {
    val rounded = coordinates.rounded()
    return SavedCityEntity(
        name = name,
        state = state,
        countryCode = countryCode,
        latitude = rounded.latitude,
        longitude = rounded.longitude,
        position = 0, // assigned by SavedCityDao.insertLast
        addedAtMillis = addedAtMillis,
    )
}
