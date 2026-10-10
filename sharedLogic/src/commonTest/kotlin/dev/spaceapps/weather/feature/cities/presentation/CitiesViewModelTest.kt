package dev.spaceapps.weather.feature.cities.presentation

import app.cash.turbine.test
import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.domain.Outcome
import dev.spaceapps.weather.core.location.Coordinates
import dev.spaceapps.weather.core.settings.DataStoreSettingsRepository
import dev.spaceapps.weather.core.settings.UnitSystem
import dev.spaceapps.weather.feature.cities.data.SavedCitiesRepositoryImpl
import dev.spaceapps.weather.feature.cities.domain.model.CityCandidate
import dev.spaceapps.weather.feature.cities.domain.model.SavedCity
import dev.spaceapps.weather.feature.cities.presentation.list.CitiesEffect
import dev.spaceapps.weather.feature.cities.presentation.list.CitiesIntent
import dev.spaceapps.weather.feature.cities.presentation.list.CitiesViewModel
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
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone

class CitiesViewModelTest {

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

    private fun viewModel() = CitiesViewModel(
        citiesRepository = citiesRepository,
        weatherRepository = weatherRepository,
        settingsRepository = settingsRepository,
        deviceTimeZone = { TimeZone.UTC },
    )

    private suspend fun save(vararg names: String): List<SavedCity> = names.mapIndexed { index, name ->
        citiesRepository.add(CityCandidate(name, null, "UA", Coordinates(40.0 + index, 30.0)))
    }

    private val SavedCity.location get() = WeatherLocation.SavedCity(id)

    @Test
    fun `empty list is reported once loaded`() = runTest {
        val viewModel = viewModel()

        assertTrue(viewModel.state.value.isLoaded)
        assertTrue(viewModel.state.value.isEmpty)
    }

    @Test
    fun `shows saved cities with their cached weather`() = runTest {
        val (lviv, odesa) = save("Lviv", "Odesa")
        weatherRepository.weather(lviv.location).value = sampleCurrentWeather()
        weatherRepository.freshLocations += listOf(lviv.location, odesa.location)

        val rows = viewModel().state.value.cities

        assertEquals(listOf("Lviv", "Odesa"), rows.map { it.name })
        assertEquals("UA", rows[0].subtitle)
        assertEquals("12°", rows[0].temperature)
        assertEquals("Broken clouds", rows[0].conditionText)
        assertEquals(null, rows[1].temperature)
    }

    @Test
    fun `cities are refreshed when they appear unless fresh`() = runTest {
        val (lviv, odesa) = save("Lviv", "Odesa")
        weatherRepository.freshLocations += lviv.location

        viewModel()

        assertEquals(listOf<WeatherLocation>(odesa.location), weatherRepository.refreshes.map { it.first })
    }

    @Test
    fun `a newly added city gets its weather fetched`() = runTest {
        viewModel()

        val added = save("Kharkiv").single()

        assertEquals(listOf<WeatherLocation>(added.location), weatherRepository.refreshes.map { it.first })
    }

    @Test
    fun `screen start refreshes only stale cities`() = runTest {
        val (lviv, odesa) = save("Lviv", "Odesa")
        weatherRepository.freshLocations += listOf(lviv.location, odesa.location)
        val viewModel = viewModel()

        weatherRepository.freshLocations -= odesa.location
        viewModel.onIntent(CitiesIntent.ScreenStarted)

        assertEquals(listOf<WeatherLocation>(odesa.location), weatherRepository.refreshes.map { it.first })
    }

    @Test
    fun `pull to refresh refreshes every city and shows the indicator`() = runTest {
        val cities = save("Lviv", "Odesa")
        weatherRepository.freshLocations += cities.map { it.location }
        val viewModel = viewModel()
        val gate = CompletableDeferred<Unit>().also { weatherRepository.refreshGate = it }

        viewModel.onIntent(CitiesIntent.Refresh)
        assertTrue(viewModel.state.value.isRefreshing)

        gate.complete(Unit)
        assertFalse(viewModel.state.value.isRefreshing)
        assertEquals(cities.map { it.location }, weatherRepository.refreshes.map { it.first })
    }

    @Test
    fun `at most four cities are fetched at once`() = runTest {
        weatherRepository.refreshGate = CompletableDeferred()
        save("A", "B", "C", "D", "E", "F")

        viewModel()

        assertEquals(4, weatherRepository.refreshes.size)
        weatherRepository.refreshGate?.complete(Unit)
        assertEquals(6, weatherRepository.refreshes.size)
    }

    @Test
    fun `failures in a batch produce a single error`() = runTest {
        weatherRepository.refreshResult = Outcome.Failure(DataError.Remote.NO_INTERNET)
        // Fresh at load, so only the pull-to-refresh batch below makes requests.
        weatherRepository.freshLocations += save("Lviv", "Odesa").map { it.location }
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onIntent(CitiesIntent.Refresh)

            assertEquals(CitiesEffect.ShowError(WeatherError.NO_INTERNET), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `delete offers undo and undo brings the city back with fresh weather`() = runTest {
        val (lviv, odesa) = save("Lviv", "Odesa")
        weatherRepository.freshLocations += listOf(lviv.location, odesa.location)
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onIntent(CitiesIntent.DeleteCity(lviv.id))
            assertEquals(CitiesEffect.ShowUndo("Lviv"), awaitItem())
        }
        assertEquals(listOf("Odesa"), viewModel.state.value.cities.map { it.name })

        weatherRepository.freshLocations -= lviv.location
        viewModel.onIntent(CitiesIntent.UndoDelete)

        assertEquals(listOf("Lviv", "Odesa"), viewModel.state.value.cities.map { it.name })
        assertEquals(listOf<WeatherLocation>(lviv.location), weatherRepository.refreshes.map { it.first })
    }

    @Test
    fun `units setting changes the temperature format`() = runTest {
        val lviv = save("Lviv").single()
        weatherRepository.weather(lviv.location).value = sampleCurrentWeather()
        weatherRepository.freshLocations += lviv.location
        val viewModel = viewModel()

        settingsRepository.setUnitSystem(UnitSystem.IMPERIAL)

        assertEquals("54°", viewModel.state.value.cities.single().temperature)
    }

    @Test
    fun `navigation intents become effects`() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onIntent(CitiesIntent.CityClicked(7))
            assertEquals(CitiesEffect.OpenCity(7), awaitItem())

            viewModel.onIntent(CitiesIntent.AddClicked)
            assertEquals(CitiesEffect.OpenSearch, awaitItem())
        }
    }
}
