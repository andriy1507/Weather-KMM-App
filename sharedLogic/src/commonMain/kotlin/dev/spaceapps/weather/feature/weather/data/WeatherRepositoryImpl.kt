package dev.spaceapps.weather.feature.weather.data

import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.domain.EmptyOutcome
import dev.spaceapps.weather.core.domain.Outcome
import dev.spaceapps.weather.core.location.Coordinates
import dev.spaceapps.weather.core.network.safeApiCall
import dev.spaceapps.weather.feature.weather.data.local.WeatherCacheDao
import dev.spaceapps.weather.feature.weather.data.mapper.toCoordinates
import dev.spaceapps.weather.feature.weather.data.mapper.toDomain
import dev.spaceapps.weather.feature.weather.data.mapper.toEntity
import dev.spaceapps.weather.feature.weather.data.remote.OpenWeatherApi
import dev.spaceapps.weather.feature.weather.domain.WeatherLocation
import dev.spaceapps.weather.feature.weather.domain.WeatherRepository
import dev.spaceapps.weather.feature.weather.domain.model.CurrentWeather
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WeatherRepositoryImpl(
    private val api: OpenWeatherApi,
    private val dao: WeatherCacheDao,
    private val clock: Clock,
) : WeatherRepository {

    override fun observeWeather(location: WeatherLocation): Flow<CurrentWeather?> =
        dao.observe(location.cacheKey).map { it?.toDomain() }

    override suspend fun refresh(
        location: WeatherLocation,
        coordinates: Coordinates,
    ): EmptyOutcome<DataError.Remote> {
        val result = safeApiCall {
            api.getCurrentWeather(coordinates.latitude, coordinates.longitude)
                .toEntity(locationKey = location.cacheKey, fetchedAt = clock.now())
        }
        return when (result) {
            is Outcome.Success -> {
                dao.upsert(result.data)
                Outcome.Success(Unit)
            }
            is Outcome.Failure -> result
        }
    }

    override suspend fun refreshIfStale(
        location: WeatherLocation,
        coordinates: Coordinates,
        maxAge: Duration,
    ): EmptyOutcome<DataError.Remote> {
        val fetchedAt = dao.get(location.cacheKey)?.fetchedAtMillis?.let(Instant::fromEpochMilliseconds)
        val isFresh = fetchedAt != null && clock.now() - fetchedAt < maxAge
        return if (isFresh) Outcome.Success(Unit) else refresh(location, coordinates)
    }

    override suspend fun cachedCoordinates(location: WeatherLocation): Coordinates? =
        dao.get(location.cacheKey)?.toCoordinates()
}
