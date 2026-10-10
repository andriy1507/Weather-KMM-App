package dev.spaceapps.weather.feature.cities.presentation.list

import androidx.lifecycle.viewModelScope
import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.domain.Outcome
import dev.spaceapps.weather.core.mvi.MviViewModel
import dev.spaceapps.weather.core.settings.SettingsRepository
import dev.spaceapps.weather.core.settings.UnitSystem
import dev.spaceapps.weather.feature.cities.domain.SavedCitiesRepository
import dev.spaceapps.weather.feature.cities.domain.model.SavedCity
import dev.spaceapps.weather.feature.cities.presentation.subtitle
import dev.spaceapps.weather.feature.weather.domain.WeatherLocation
import dev.spaceapps.weather.feature.weather.domain.WeatherRepository
import dev.spaceapps.weather.feature.weather.domain.model.CurrentWeather
import dev.spaceapps.weather.feature.weather.presentation.toUi
import dev.spaceapps.weather.feature.weather.presentation.toWeatherError
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.TimeZone

class CitiesViewModel(
    private val citiesRepository: SavedCitiesRepository,
    private val weatherRepository: WeatherRepository,
    settingsRepository: SettingsRepository,
    private val staleAfter: Duration = 10.minutes,
    private val deviceTimeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : MviViewModel<CitiesState, CitiesIntent, CitiesEffect>(CitiesState()) {

    private var cities: List<SavedCity> = emptyList()

    /** Cities already handed to auto-refresh; a city added (or restored) later is refreshed on arrival. */
    private val knownCityIds = mutableSetOf<Long>()

    /** Cities with a refresh in flight, so overlapping triggers don't fetch the same city twice. */
    private val refreshingCityIds = mutableSetOf<Long>()

    /** Caps parallel requests (OpenWeather free tier: 60 calls/minute). */
    private val requestPermits = Semaphore(MAX_PARALLEL_REQUESTS)

    private var lastRemoved: SavedCity? = null

    init {
        @OptIn(ExperimentalCoroutinesApi::class)
        val citiesWithWeather = citiesRepository.observeCities()
            .onEach(::onCitiesChanged)
            .flatMapLatest { cities -> cities.withCachedWeather() }

        combine(citiesWithWeather, settingsRepository.settings) { rows, settings ->
            rows.map { (city, weather) -> city.toRow(weather, settings.unitSystem) }
        }
            .onEach { rows -> setState { copy(cities = rows, isLoaded = true) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: CitiesIntent) {
        when (intent) {
            CitiesIntent.ScreenStarted -> refresh(cities, force = false, showIndicator = false)
            CitiesIntent.Refresh -> refresh(cities, force = true, showIndicator = true)
            is CitiesIntent.CityClicked -> sendEffect(CitiesEffect.OpenCity(intent.cityId))
            CitiesIntent.AddClicked -> sendEffect(CitiesEffect.OpenSearch)
            is CitiesIntent.DeleteCity -> delete(intent.cityId)
            CitiesIntent.UndoDelete -> undoDelete()
        }
    }

    private fun onCitiesChanged(cities: List<SavedCity>) {
        this.cities = cities
        val added = cities.filter { knownCityIds.add(it.id) }
        refresh(added, force = false, showIndicator = false)
    }

    private fun refresh(targets: List<SavedCity>, force: Boolean, showIndicator: Boolean) {
        val toRefresh = targets.filter { it.id !in refreshingCityIds }
        if (toRefresh.isEmpty()) return
        refreshingCityIds += toRefresh.map { it.id }

        viewModelScope.launch {
            if (showIndicator) setState { copy(isRefreshing = true) }
            val results = try {
                coroutineScope {
                    toRefresh.map { city -> async { requestPermits.withPermit { refreshCity(city, force) } } }
                        .awaitAll()
                }
            } finally {
                refreshingCityIds -= toRefresh.map { it.id }.toSet()
                if (showIndicator) setState { copy(isRefreshing = false) }
            }
            // One message for the batch: several cities usually fail for the same reason (e.g. offline).
            results.filterIsInstance<Outcome.Failure<DataError.Remote>>().firstOrNull()?.let {
                sendEffect(CitiesEffect.ShowError(it.error.toWeatherError()))
            }
        }
    }

    private suspend fun refreshCity(city: SavedCity, force: Boolean): Outcome<Unit, DataError.Remote> {
        val location = WeatherLocation.SavedCity(city.id)
        return if (force) {
            weatherRepository.refresh(location, city.coordinates)
        } else {
            weatherRepository.refreshIfStale(location, city.coordinates, staleAfter)
        }
    }

    private fun delete(cityId: Long) {
        viewModelScope.launch {
            val removed = citiesRepository.remove(cityId) ?: return@launch
            lastRemoved = removed
            // Its cached weather is gone; if the deletion is undone it gets fetched again.
            knownCityIds -= cityId
            sendEffect(CitiesEffect.ShowUndo(removed.name))
        }
    }

    private fun undoDelete() {
        val city = lastRemoved ?: return
        lastRemoved = null
        viewModelScope.launch { citiesRepository.restore(city) }
    }

    private fun List<SavedCity>.withCachedWeather(): Flow<List<Pair<SavedCity, CurrentWeather?>>> =
        if (isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(
                map { city ->
                    weatherRepository.observeWeather(WeatherLocation.SavedCity(city.id)).map { city to it }
                },
            ) { it.toList() }
        }

    private fun SavedCity.toRow(weather: CurrentWeather?, unitSystem: UnitSystem): CityRowUi {
        val weatherUi = weather?.toUi(unitSystem, deviceTimeZone())
        return CityRowUi(
            id = id,
            name = name,
            subtitle = subtitle,
            temperature = weatherUi?.temperature,
            conditionText = weatherUi?.conditionText,
            iconUrl = weatherUi?.iconUrl,
        )
    }

    private companion object {
        const val MAX_PARALLEL_REQUESTS = 4
    }
}
