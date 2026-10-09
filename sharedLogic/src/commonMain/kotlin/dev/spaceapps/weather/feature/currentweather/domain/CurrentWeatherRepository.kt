package dev.spaceapps.weather.feature.currentweather.domain

import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.domain.EmptyOutcome
import dev.spaceapps.weather.core.location.Coordinates
import dev.spaceapps.weather.feature.currentweather.domain.model.CurrentWeather
import kotlinx.coroutines.flow.Flow

/** Offline-first: [observeCurrentWeather] emits the cache, [refresh] updates it from the network. */
interface CurrentWeatherRepository {
    fun observeCurrentWeather(): Flow<CurrentWeather?>

    suspend fun refresh(coordinates: Coordinates): EmptyOutcome<DataError.Remote>

    /** Coordinates of the last successful fetch, used when no fresh location fix is available. */
    suspend fun lastKnownCoordinates(): Coordinates?
}
