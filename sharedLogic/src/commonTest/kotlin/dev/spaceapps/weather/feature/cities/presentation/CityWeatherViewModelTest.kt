package dev.spaceapps.weather.feature.cities.presentation

import app.cash.turbine.test
import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.domain.Outcome
import dev.spaceapps.weather.core.location.Coordinates
import dev.spaceapps.weather.core.settings.DataStoreSettingsRepository
import dev.spaceapps.weather.feature.cities.data.SavedCitiesRepositoryImpl
import dev.spaceapps.weather.feature.cities.domain.model.CityCandidate
import dev.spaceapps.weather.feature.cities.domain.model.SavedCity
import dev.spaceapps.weather.feature.cities.presentation.detail.CityWeatherEffect
import dev.spaceapps.weather.feature.cities.presentation.detail.CityWeatherIntent
import dev.spaceapps.weather.feature.cities.presentation.detail.CityWeatherViewModel
import dev.spaceapps.weather.feature.weather.domain.WeatherLocation
import dev.spaceapps.weather.feature.weather.presentation.WeatherError
import dev.spaceapps.weather.testing.FakePreferencesDataStore
import dev.spaceapps.weather.testing.FakeSavedCityDao
import dev.spaceapps.weather.testing.FakeWeatherRepository
import dev.spaceapps.weather.testing.FixedClock
import dev.spaceapps.weather.testing.sampleCurrentWeather
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone

class CityWeatherViewModelTest {

    private val citiesRepository = SavedCitiesRepositoryImpl(FakeSavedCityDao(), FixedClock(Instant.fromEpochMilliseconds(0)))
    private val weatherRepository = FakeWeatherRepository()
    private val settingsRepository = DataStoreSettingsRepository(FakePreferencesDataStore())

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun saveSpringfield(): SavedCity =
        citiesRepository.add(CityCandidate("Springfield", "Illinois", "US", Coordinates(39.799, -89.644)))

    private fun viewModel(cityId: Long) = CityWeatherViewModel(
        cityId = cityId,
        citiesRepository = citiesRepository,
        weatherRepository = weatherRepository,
        settingsRepository = settingsRepository,
        deviceTimeZone = { TimeZone.UTC },
    )

    @Test
    fun `shows the saved name instead of the weather station name`() = runTest {
        val city = saveSpringfield()
        weatherRepository.weather(WeatherLocation.SavedCity(city.id)).value = sampleCurrentWeather()

        val state = viewModel(city.id).state.value

        assertEquals("Springfield", state.title)
        assertEquals("Illinois, US", state.subtitle)
        assertEquals("Springfield", state.weather?.location)
        assertEquals("12°", state.weather?.temperature)
    }

    @Test
    fun `screen start refreshes only stale weather and refresh always fetches`() = runTest {
        val city = saveSpringfield()
        val location: WeatherLocation = WeatherLocation.SavedCity(city.id)
        weatherRepository.freshLocations += location
        val viewModel = viewModel(city.id)

        viewModel.onIntent(CityWeatherIntent.ScreenStarted)
        assertTrue(weatherRepository.refreshes.isEmpty())

        viewModel.onIntent(CityWeatherIntent.Refresh)
        assertEquals(listOf(location to city.coordinates), weatherRepository.refreshes)
    }

    @Test
    fun `failure without cached weather shows the error state`() = runTest {
        val city = saveSpringfield()
        weatherRepository.refreshResult = Outcome.Failure(DataError.Remote.SERVER)
        val viewModel = viewModel(city.id)

        viewModel.onIntent(CityWeatherIntent.ScreenStarted)

        assertEquals(WeatherError.SERVER, viewModel.state.value.error)
        assertNull(viewModel.state.value.weather)
    }

    @Test
    fun `delete asks for confirmation then removes the city and closes`() = runTest {
        val city = saveSpringfield()
        val viewModel = viewModel(city.id)

        viewModel.onIntent(CityWeatherIntent.DeleteClicked)
        assertTrue(viewModel.state.value.showDeleteConfirmation)
        viewModel.onIntent(CityWeatherIntent.DeleteDismissed)
        assertFalse(viewModel.state.value.showDeleteConfirmation)

        viewModel.effects.test {
            viewModel.onIntent(CityWeatherIntent.DeleteClicked)
            viewModel.onIntent(CityWeatherIntent.DeleteConfirmed)

            assertEquals(CityWeatherEffect.Close, awaitItem())
            expectNoEvents()
        }
        assertNull(citiesRepository.getCity(city.id))
    }

    @Test
    fun `missing city closes the screen`() = runTest {
        val viewModel = viewModel(cityId = 99)

        viewModel.effects.test {
            assertEquals(CityWeatherEffect.Close, awaitItem())
        }
    }
}
