package dev.spaceapps.weather.testing

import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.domain.EmptyOutcome
import dev.spaceapps.weather.core.domain.Outcome
import dev.spaceapps.weather.core.location.Coordinates
import dev.spaceapps.weather.core.location.LocationError
import dev.spaceapps.weather.core.location.LocationProvider
import dev.spaceapps.weather.feature.cities.domain.CitySearchRepository
import dev.spaceapps.weather.feature.cities.domain.model.CitySearchResult
import dev.spaceapps.weather.feature.weather.domain.WeatherLocation
import dev.spaceapps.weather.feature.weather.domain.WeatherRepository
import dev.spaceapps.weather.feature.weather.domain.model.CurrentWeather
import kotlin.time.Duration
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

/** In-memory [WeatherRepository]; [cached] seeds the current-location entry. */
class FakeWeatherRepository(cached: CurrentWeather? = null) : WeatherRepository {
    private val cache = mutableMapOf<WeatherLocation, MutableStateFlow<CurrentWeather?>>()

    val refreshes = mutableListOf<Pair<WeatherLocation, Coordinates>>()
    val refreshedCoordinates: List<Coordinates> get() = refreshes.map { it.second }

    var refreshResult: EmptyOutcome<DataError.Remote> = Outcome.Success(Unit)

    /** Stored for the refreshed location on a successful refresh. */
    var fetchedWeather: CurrentWeather? = sampleCurrentWeather()

    /** When set, refresh suspends until completed (to observe in-flight state). */
    var refreshGate: CompletableDeferred<Unit>? = null

    init {
        weather(WeatherLocation.Current).value = cached
    }

    fun weather(location: WeatherLocation): MutableStateFlow<CurrentWeather?> =
        cache.getOrPut(location) { MutableStateFlow(null) }

    override fun observeWeather(location: WeatherLocation): Flow<CurrentWeather?> = weather(location)

    override suspend fun refresh(location: WeatherLocation, coordinates: Coordinates): EmptyOutcome<DataError.Remote> {
        refreshes += location to coordinates
        refreshGate?.await()
        if (refreshResult is Outcome.Success) fetchedWeather?.let { weather(location).value = it }
        return refreshResult
    }

    /** Locations [refreshIfStale] treats as fresh (no refresh). */
    val freshLocations = mutableSetOf<WeatherLocation>()

    override suspend fun refreshIfStale(
        location: WeatherLocation,
        coordinates: Coordinates,
        maxAge: Duration,
    ): EmptyOutcome<DataError.Remote> =
        if (location in freshLocations) Outcome.Success(Unit) else refresh(location, coordinates)

    override suspend fun cachedCoordinates(location: WeatherLocation): Coordinates? =
        weather(location).value?.coordinates
}

class FakeCitySearchRepository : CitySearchRepository {
    val queries = mutableListOf<String>()
    var results: Outcome<List<CitySearchResult>, DataError.Remote> = Outcome.Success(emptyList())

    /** When set, searches for this query suspend until completed. */
    var gatedQuery: String? = null
    val gate = CompletableDeferred<Unit>()

    override suspend fun search(query: String): Outcome<List<CitySearchResult>, DataError.Remote> {
        queries += query
        if (query == gatedQuery) gate.await()
        return results
    }
}
