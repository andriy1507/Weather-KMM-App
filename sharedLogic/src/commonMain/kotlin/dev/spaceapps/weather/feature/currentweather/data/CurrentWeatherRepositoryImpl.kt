package dev.spaceapps.weather.feature.currentweather.data

import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.domain.EmptyOutcome
import dev.spaceapps.weather.core.domain.Outcome
import dev.spaceapps.weather.core.network.safeApiCall
import dev.spaceapps.weather.feature.currentweather.data.local.CurrentWeatherDao
import dev.spaceapps.weather.feature.currentweather.data.mapper.toCoordinates
import dev.spaceapps.weather.feature.currentweather.data.mapper.toDomain
import dev.spaceapps.weather.feature.currentweather.data.mapper.toEntity
import dev.spaceapps.weather.feature.currentweather.data.remote.OpenWeatherApi
import dev.spaceapps.weather.feature.currentweather.domain.CurrentWeatherRepository
import dev.spaceapps.weather.core.location.Coordinates
import dev.spaceapps.weather.feature.currentweather.domain.model.CurrentWeather
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CurrentWeatherRepositoryImpl(
    private val api: OpenWeatherApi,
    private val dao: CurrentWeatherDao,
    private val clock: Clock,
) : CurrentWeatherRepository {

    override fun observeCurrentWeather(): Flow<CurrentWeather?> =
        dao.observe().map { it?.toDomain() }

    override suspend fun refresh(coordinates: Coordinates): EmptyOutcome<DataError.Remote> {
        val result = safeApiCall {
            api.getCurrentWeather(coordinates.latitude, coordinates.longitude)
                .toEntity(fetchedAt = clock.now())
        }
        return when (result) {
            is Outcome.Success -> {
                dao.upsert(result.data)
                Outcome.Success(Unit)
            }
            is Outcome.Failure -> result
        }
    }

    override suspend fun lastKnownCoordinates(): Coordinates? = dao.get()?.toCoordinates()
}
