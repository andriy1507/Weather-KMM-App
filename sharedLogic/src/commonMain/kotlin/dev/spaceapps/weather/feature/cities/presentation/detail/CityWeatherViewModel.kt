package dev.spaceapps.weather.feature.cities.presentation.detail

import androidx.lifecycle.viewModelScope
import dev.spaceapps.weather.core.domain.Outcome
import dev.spaceapps.weather.core.mvi.MviViewModel
import dev.spaceapps.weather.core.settings.SettingsRepository
import dev.spaceapps.weather.feature.cities.domain.SavedCitiesRepository
import dev.spaceapps.weather.feature.cities.presentation.subtitle
import dev.spaceapps.weather.feature.weather.domain.WeatherLocation
import dev.spaceapps.weather.feature.weather.domain.WeatherRepository
import dev.spaceapps.weather.feature.weather.presentation.toUi
import dev.spaceapps.weather.feature.weather.presentation.toWeatherError
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

/** Weather of one saved city ([cityId]); the same display as the current-location tab. */
class CityWeatherViewModel(
    private val cityId: Long,
    private val citiesRepository: SavedCitiesRepository,
    private val weatherRepository: WeatherRepository,
    settingsRepository: SettingsRepository,
    private val staleAfter: Duration = 10.minutes,
    private val deviceTimeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : MviViewModel<CityWeatherState, CityWeatherIntent, CityWeatherEffect>(CityWeatherState()) {

    private val location = WeatherLocation.SavedCity(cityId)
    private var refreshJob: Job? = null
    private var closed = false

    init {
        combine(
            citiesRepository.observeCities().map { cities -> cities.find { it.id == cityId } },
            weatherRepository.observeWeather(location),
            settingsRepository.settings,
        ) { city, weather, settings ->
            if (city == null) {
                close()
            } else {
                setState {
                    copy(
                        title = city.name,
                        subtitle = city.subtitle,
                        // The saved name ("Lviv") rather than the weather station name from the API.
                        weather = weather?.toUi(settings.unitSystem, deviceTimeZone())?.copy(location = city.name),
                        error = if (weather != null) null else error,
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: CityWeatherIntent) {
        when (intent) {
            CityWeatherIntent.ScreenStarted -> refresh(force = false)
            CityWeatherIntent.Refresh -> refresh(force = true)
            CityWeatherIntent.DeleteClicked -> setState { copy(showDeleteConfirmation = true) }
            CityWeatherIntent.DeleteDismissed -> setState { copy(showDeleteConfirmation = false) }
            CityWeatherIntent.DeleteConfirmed -> delete()
        }
    }

    private fun refresh(force: Boolean) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            val city = citiesRepository.getCity(cityId) ?: return@launch close()
            setState { copy(isFetching = true) }
            val result = if (force) {
                weatherRepository.refresh(location, city.coordinates)
            } else {
                weatherRepository.refreshIfStale(location, city.coordinates, staleAfter)
            }
            setState { copy(isFetching = false) }

            when (result) {
                is Outcome.Success -> setState { copy(error = null) }
                is Outcome.Failure -> {
                    val error = result.error.toWeatherError()
                    if (currentState.weather == null) {
                        setState { copy(error = error) }
                    } else {
                        sendEffect(CityWeatherEffect.ShowError(error))
                    }
                }
            }
        }
    }

    private fun delete() {
        setState { copy(showDeleteConfirmation = false) }
        viewModelScope.launch {
            citiesRepository.remove(cityId)
            close()
        }
    }

    private fun close() {
        if (closed) return
        closed = true
        sendEffect(CityWeatherEffect.Close)
    }
}
