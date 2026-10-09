package dev.spaceapps.weather.testing

import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.domain.EmptyOutcome
import dev.spaceapps.weather.core.domain.Outcome
import dev.spaceapps.weather.core.location.Coordinates
import dev.spaceapps.weather.core.location.LocationError
import dev.spaceapps.weather.core.location.LocationProvider
import dev.spaceapps.weather.feature.currentweather.domain.CurrentWeatherRepository
import dev.spaceapps.weather.feature.currentweather.domain.model.CurrentWeather
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeLocationProvider(
    var hasPermission: Boolean = true,
    var location: Outcome<Coordinates, LocationError> = Outcome.Success(Coordinates(50.45, 30.52)),
) : LocationProvider {
    override fun hasLocationPermission(): Boolean = hasPermission

    override suspend fun getCurrentLocation(): Outcome<Coordinates, LocationError> = location
}

class FakeCurrentWeatherRepository(cached: CurrentWeather? = null) : CurrentWeatherRepository {
    val weather = MutableStateFlow(cached)
    val refreshedCoordinates = mutableListOf<Coordinates>()

    var refreshResult: EmptyOutcome<DataError.Remote> = Outcome.Success(Unit)

    /** Emitted on a successful refresh. */
    var fetchedWeather: CurrentWeather? = sampleCurrentWeather()

    /** When set, refresh suspends until completed (to observe in-flight state). */
    var refreshGate: CompletableDeferred<Unit>? = null

    override fun observeCurrentWeather(): Flow<CurrentWeather?> = weather

    override suspend fun refresh(coordinates: Coordinates): EmptyOutcome<DataError.Remote> {
        refreshedCoordinates += coordinates
        refreshGate?.await()
        if (refreshResult is Outcome.Success) fetchedWeather?.let { weather.value = it }
        return refreshResult
    }

    override suspend fun lastKnownCoordinates(): Coordinates? = weather.value?.coordinates
}
