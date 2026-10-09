package dev.spaceapps.weather.feature.currentweather.domain

import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.domain.Outcome
import dev.spaceapps.weather.core.location.Coordinates
import dev.spaceapps.weather.core.location.LocationError
import dev.spaceapps.weather.feature.currentweather.domain.usecase.RefreshCurrentWeatherUseCase
import dev.spaceapps.weather.feature.currentweather.domain.usecase.RefreshError
import dev.spaceapps.weather.testing.FakeCurrentWeatherRepository
import dev.spaceapps.weather.testing.FakeLocationProvider
import dev.spaceapps.weather.testing.sampleCurrentWeather
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class RefreshCurrentWeatherUseCaseTest {

    private val deviceLocation = Coordinates(48.0, 24.0)
    private val locationProvider = FakeLocationProvider(location = Outcome.Success(deviceLocation))

    @Test
    fun `refreshes weather at the current device location`() = runTest {
        val repository = FakeCurrentWeatherRepository(cached = sampleCurrentWeather())

        val result = RefreshCurrentWeatherUseCase(locationProvider, repository)()

        assertEquals(Outcome.Success(Unit), result)
        assertEquals(listOf(deviceLocation), repository.refreshedCoordinates)
    }

    @Test
    fun `falls back to last known coordinates when location is unavailable`() = runTest {
        val cached = sampleCurrentWeather()
        val repository = FakeCurrentWeatherRepository(cached = cached)
        locationProvider.location = Outcome.Failure(LocationError.PERMISSION_DENIED)

        val result = RefreshCurrentWeatherUseCase(locationProvider, repository)()

        assertEquals(Outcome.Success(Unit), result)
        assertEquals(listOf(cached.coordinates), repository.refreshedCoordinates)
    }

    @Test
    fun `fails with location error when there is no location and no cache`() = runTest {
        val repository = FakeCurrentWeatherRepository()
        locationProvider.location = Outcome.Failure(LocationError.LOCATION_DISABLED)

        val result = RefreshCurrentWeatherUseCase(locationProvider, repository)()

        assertEquals(Outcome.Failure(RefreshError.Location(LocationError.LOCATION_DISABLED)), result)
        assertTrue(repository.refreshedCoordinates.isEmpty())
    }

    @Test
    fun `wraps remote failures`() = runTest {
        val repository = FakeCurrentWeatherRepository()
        repository.refreshResult = Outcome.Failure(DataError.Remote.NO_INTERNET)

        val result = RefreshCurrentWeatherUseCase(locationProvider, repository)()

        assertEquals(Outcome.Failure(RefreshError.Remote(DataError.Remote.NO_INTERNET)), result)
    }
}
