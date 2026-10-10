package dev.spaceapps.weather.feature.currentweather.domain.usecase

import dev.spaceapps.weather.core.domain.AppError
import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.domain.EmptyOutcome
import dev.spaceapps.weather.core.domain.Outcome
import dev.spaceapps.weather.core.location.LocationError
import dev.spaceapps.weather.core.location.LocationProvider
import dev.spaceapps.weather.feature.weather.domain.WeatherLocation
import dev.spaceapps.weather.feature.weather.domain.WeatherRepository

sealed interface RefreshError : AppError {
    data class Location(val error: LocationError) : RefreshError
    data class Remote(val error: DataError.Remote) : RefreshError
}

/**
 * Fetches weather for the current device location. When the location can't be obtained
 * (no permission, GPS off, no fix), falls back to the coordinates of the last successful fetch.
 */
class RefreshCurrentWeatherUseCase(
    private val locationProvider: LocationProvider,
    private val repository: WeatherRepository,
) {
    suspend operator fun invoke(): EmptyOutcome<RefreshError> {
        val coordinates = when (val location = locationProvider.getCurrentLocation()) {
            is Outcome.Success -> location.data
            is Outcome.Failure -> repository.cachedCoordinates(WeatherLocation.Current)
                ?: return Outcome.Failure(RefreshError.Location(location.error))
        }
        return when (val result = repository.refresh(WeatherLocation.Current, coordinates)) {
            is Outcome.Success -> result
            is Outcome.Failure -> Outcome.Failure(RefreshError.Remote(result.error))
        }
    }
}
