package dev.spaceapps.weather.feature.currentweather.presentation

import app.cash.turbine.test
import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.domain.Outcome
import dev.spaceapps.weather.core.location.LocationError
import dev.spaceapps.weather.core.settings.DataStoreSettingsRepository
import dev.spaceapps.weather.core.settings.UnitSystem
import dev.spaceapps.weather.feature.currentweather.domain.usecase.ObserveCurrentWeatherUseCase
import dev.spaceapps.weather.feature.currentweather.domain.usecase.RefreshCurrentWeatherUseCase
import dev.spaceapps.weather.feature.weather.presentation.WeatherError
import dev.spaceapps.weather.testing.FakeLocationProvider
import dev.spaceapps.weather.testing.FakePreferencesDataStore
import dev.spaceapps.weather.testing.FakeWeatherRepository
import dev.spaceapps.weather.testing.sampleCurrentWeather
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone

class CurrentWeatherViewModelTest {

    private val locationProvider = FakeLocationProvider()
    private val settingsRepository = DataStoreSettingsRepository(FakePreferencesDataStore())

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeWeatherRepository) = CurrentWeatherViewModel(
        observeCurrentWeather = ObserveCurrentWeatherUseCase(repository),
        refreshCurrentWeather = RefreshCurrentWeatherUseCase(locationProvider, repository),
        locationProvider = locationProvider,
        settingsRepository = settingsRepository,
        deviceTimeZone = { TimeZone.UTC },
    )

    @Test
    fun `shows cached weather immediately`() = runTest {
        val viewModel = viewModel(FakeWeatherRepository(cached = sampleCurrentWeather()))

        assertEquals("12°", viewModel.state.value.weather?.temperature)
    }

    @Test
    fun `screen start with permission refreshes weather`() = runTest {
        val repository = FakeWeatherRepository()
        val viewModel = viewModel(repository)

        viewModel.onIntent(CurrentWeatherIntent.ScreenStarted)

        val state = viewModel.state.value
        assertNotNull(state.weather)
        assertEquals(LocationPermission.GRANTED, state.permission)
        assertFalse(state.isFetching)
        assertNull(state.error)
        assertEquals(1, repository.refreshedCoordinates.size)
    }

    @Test
    fun `screen start is handled once`() = runTest {
        val repository = FakeWeatherRepository()
        val viewModel = viewModel(repository)

        viewModel.onIntent(CurrentWeatherIntent.ScreenStarted)
        viewModel.onIntent(CurrentWeatherIntent.ScreenStarted)

        assertEquals(1, repository.refreshedCoordinates.size)
    }

    @Test
    fun `screen start without permission requests it instead of refreshing`() = runTest {
        val repository = FakeWeatherRepository()
        locationProvider.hasPermission = false
        val viewModel = viewModel(repository)

        viewModel.effects.test {
            viewModel.onIntent(CurrentWeatherIntent.ScreenStarted)

            assertEquals(CurrentWeatherEffect.RequestLocationPermission, awaitItem())
        }
        assertTrue(repository.refreshedCoordinates.isEmpty())
    }

    @Test
    fun `denied permission without cache shows permission error`() = runTest {
        locationProvider.hasPermission = false
        locationProvider.location = Outcome.Failure(LocationError.PERMISSION_DENIED)
        val viewModel = viewModel(FakeWeatherRepository())

        viewModel.onIntent(CurrentWeatherIntent.PermissionResult(granted = false))

        val state = viewModel.state.value
        assertEquals(LocationPermission.DENIED, state.permission)
        assertEquals(WeatherError.LOCATION_PERMISSION_DENIED, state.error)
        assertNull(state.weather)
    }

    @Test
    fun `granted permission refreshes weather`() = runTest {
        val repository = FakeWeatherRepository()
        val viewModel = viewModel(repository)

        viewModel.onIntent(CurrentWeatherIntent.PermissionResult(granted = true))

        assertEquals(LocationPermission.GRANTED, viewModel.state.value.permission)
        assertNotNull(viewModel.state.value.weather)
    }

    @Test
    fun `failure with cached weather keeps it and emits a one-off error`() = runTest {
        val repository = FakeWeatherRepository(cached = sampleCurrentWeather())
        repository.refreshResult = Outcome.Failure(DataError.Remote.NO_INTERNET)
        val viewModel = viewModel(repository)

        viewModel.effects.test {
            viewModel.onIntent(CurrentWeatherIntent.Refresh)

            assertEquals(CurrentWeatherEffect.ShowError(WeatherError.NO_INTERNET), awaitItem())
        }
        assertNotNull(viewModel.state.value.weather)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `failure without cache shows error state until a refresh succeeds`() = runTest {
        val repository = FakeWeatherRepository()
        repository.refreshResult = Outcome.Failure(DataError.Remote.UNAUTHORIZED)
        val viewModel = viewModel(repository)

        viewModel.onIntent(CurrentWeatherIntent.Refresh)
        assertEquals(WeatherError.INVALID_API_KEY, viewModel.state.value.error)

        repository.refreshResult = Outcome.Success(Unit)
        viewModel.onIntent(CurrentWeatherIntent.Refresh)
        assertNull(viewModel.state.value.error)
        assertNotNull(viewModel.state.value.weather)
    }

    @Test
    fun `loading without cache and refreshing with cache without concurrent refreshes`() = runTest {
        val repository = FakeWeatherRepository()
        val gate = CompletableDeferred<Unit>().also { repository.refreshGate = it }
        val viewModel = viewModel(repository)

        viewModel.onIntent(CurrentWeatherIntent.Refresh)
        viewModel.onIntent(CurrentWeatherIntent.Refresh)
        assertTrue(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isRefreshing)
        assertEquals(1, repository.refreshedCoordinates.size)

        gate.complete(Unit)
        assertFalse(viewModel.state.value.isLoading)

        repository.refreshGate = CompletableDeferred()
        viewModel.onIntent(CurrentWeatherIntent.Refresh)
        assertTrue(viewModel.state.value.isRefreshing)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `changing units in settings reformats weather`() = runTest {
        val viewModel = viewModel(FakeWeatherRepository(cached = sampleCurrentWeather()))

        settingsRepository.setUnitSystem(UnitSystem.IMPERIAL)

        assertEquals("54°", viewModel.state.value.weather?.temperature)
    }

    @Test
    fun `grant permission click requests permission`() = runTest {
        val viewModel = viewModel(FakeWeatherRepository())

        viewModel.effects.test {
            viewModel.onIntent(CurrentWeatherIntent.GrantPermissionClicked)

            assertEquals(CurrentWeatherEffect.RequestLocationPermission, awaitItem())
        }
    }
}
