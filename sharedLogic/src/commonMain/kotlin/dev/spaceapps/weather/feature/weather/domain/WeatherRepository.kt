package dev.spaceapps.weather.feature.weather.domain

import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.domain.EmptyOutcome
import dev.spaceapps.weather.core.location.Coordinates
import dev.spaceapps.weather.feature.weather.domain.model.CurrentWeather
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow

/** Offline-first: [observeWeather] emits the cache, [refresh] updates it from the network. */
interface WeatherRepository {
    fun observeWeather(location: WeatherLocation): Flow<CurrentWeather?>

    suspend fun refresh(location: WeatherLocation, coordinates: Coordinates): EmptyOutcome<DataError.Remote>

    /** Like [refresh], but skips the network call when the cached weather is younger than [maxAge]. */
    suspend fun refreshIfStale(
        location: WeatherLocation,
        coordinates: Coordinates,
        maxAge: Duration,
    ): EmptyOutcome<DataError.Remote>

    /** Coordinates of the last successful fetch for [location], if any. */
    suspend fun cachedCoordinates(location: WeatherLocation): Coordinates?
}
