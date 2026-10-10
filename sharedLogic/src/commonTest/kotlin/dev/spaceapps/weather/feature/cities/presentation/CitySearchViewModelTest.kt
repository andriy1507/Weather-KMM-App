package dev.spaceapps.weather.feature.cities.presentation

import app.cash.turbine.test
import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.domain.Outcome
import dev.spaceapps.weather.core.location.Coordinates
import dev.spaceapps.weather.feature.cities.data.SavedCitiesRepositoryImpl
import dev.spaceapps.weather.feature.cities.domain.model.CityCandidate
import dev.spaceapps.weather.feature.cities.domain.model.CitySearchResult
import dev.spaceapps.weather.feature.cities.presentation.search.CitySearchEffect
import dev.spaceapps.weather.feature.cities.presentation.search.CitySearchIntent
import dev.spaceapps.weather.feature.cities.presentation.search.CitySearchViewModel
import dev.spaceapps.weather.feature.weather.presentation.WeatherError
import dev.spaceapps.weather.testing.FakeCitySearchRepository
import dev.spaceapps.weather.testing.FakeSavedCityDao
import dev.spaceapps.weather.testing.FixedClock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class CitySearchViewModelTest {

    private val searchRepository = FakeCitySearchRepository()
    private val citiesRepository = SavedCitiesRepositoryImpl(FakeSavedCityDao(), FixedClock(Instant.fromEpochMilliseconds(0)))

    private val springfield = CityCandidate("Springfield", "Illinois", "US", Coordinates(39.799, -89.644))
    private val springfieldMo = CityCandidate("Springfield", "Missouri", "US", Coordinates(37.2153, -93.2982))

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = CitySearchViewModel(searchRepository, citiesRepository)

    private fun CitySearchViewModel.type(query: String) = onIntent(CitySearchIntent.QueryChanged(query))

    /** Lets the 350 ms typing debounce pass. */
    private fun TestScope.pause() {
        advanceTimeBy(351)
        runCurrent()
    }

    @Test
    fun `searches once typing pauses`() = runTest {
        val viewModel = viewModel()

        viewModel.type("Spr")
        advanceTimeBy(200)
        viewModel.type("Spring")
        advanceTimeBy(349)
        assertTrue(searchRepository.queries.isEmpty())

        pause()
        assertEquals(listOf("Spring"), searchRepository.queries)
        assertEquals("Spring", viewModel.state.value.query)
    }

    @Test
    fun `results are shown with subtitle and saved marker`() = runTest {
        searchRepository.results = Outcome.Success(
            listOf(CitySearchResult(springfield, isSaved = false), CitySearchResult(springfieldMo, isSaved = true)),
        )
        val viewModel = viewModel()

        viewModel.type("Springfield")
        pause()

        val results = viewModel.state.value.results
        assertEquals(listOf("Illinois, US", "Missouri, US"), results.map { it.subtitle })
        assertEquals(listOf(false, true), results.map { it.isSaved })
        assertFalse(viewModel.state.value.isSearching)
        assertFalse(viewModel.state.value.showNoResults)
    }

    @Test
    fun `no matches shows the empty message`() = runTest {
        val viewModel = viewModel()

        viewModel.type("Qwxyz")
        pause()

        assertTrue(viewModel.state.value.showNoResults)
    }

    @Test
    fun `queries shorter than two characters clear results without searching`() = runTest {
        searchRepository.results = Outcome.Success(listOf(CitySearchResult(springfield, isSaved = false)))
        val viewModel = viewModel()
        viewModel.type("Springfield")
        pause()

        viewModel.type("S")
        runCurrent()

        assertEquals(emptyList(), viewModel.state.value.results)
        assertEquals(listOf("Springfield"), searchRepository.queries)
    }

    @Test
    fun `a newer query replaces a search still in progress`() = runTest {
        searchRepository.gatedQuery = "Spring"
        val viewModel = viewModel()
        viewModel.type("Spring")
        pause()
        assertTrue(viewModel.state.value.isSearching)

        searchRepository.results = Outcome.Success(listOf(CitySearchResult(springfieldMo, isSaved = false)))
        viewModel.type("Springfield")
        pause()
        searchRepository.gate.complete(Unit)

        assertEquals(listOf("Spring", "Springfield"), searchRepository.queries)
        assertEquals(listOf("Missouri, US"), viewModel.state.value.results.map { it.subtitle })
        assertFalse(viewModel.state.value.isSearching)
    }

    @Test
    fun `failure shows an error and retry searches again`() = runTest {
        searchRepository.results = Outcome.Failure(DataError.Remote.NO_INTERNET)
        val viewModel = viewModel()
        viewModel.type("Lviv")
        pause()
        assertEquals(WeatherError.NO_INTERNET, viewModel.state.value.error)

        searchRepository.results = Outcome.Success(listOf(CitySearchResult(springfield, isSaved = false)))
        viewModel.onIntent(CitySearchIntent.Retry)

        assertNull(viewModel.state.value.error)
        assertEquals(1, viewModel.state.value.results.size)
        assertEquals(listOf("Lviv", "Lviv"), searchRepository.queries)
    }

    @Test
    fun `clicking a result saves the city and closes search`() = runTest {
        searchRepository.results = Outcome.Success(listOf(CitySearchResult(springfield, isSaved = false)))
        val viewModel = viewModel()
        viewModel.type("Springfield")
        pause()

        viewModel.effects.test {
            viewModel.onIntent(CitySearchIntent.ResultClicked(viewModel.state.value.results.single().id))

            val saved = citiesRepository.getCity(1)
            assertEquals("Springfield", saved?.name)
            assertEquals(CitySearchEffect.CityAdded(1), awaitItem())
        }
    }
}
