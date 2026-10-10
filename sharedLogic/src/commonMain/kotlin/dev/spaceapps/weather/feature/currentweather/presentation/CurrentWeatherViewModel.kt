package dev.spaceapps.weather.feature.currentweather.presentation

import androidx.lifecycle.viewModelScope
import dev.spaceapps.weather.core.domain.Outcome
import dev.spaceapps.weather.core.location.LocationProvider
import dev.spaceapps.weather.core.mvi.MviViewModel
import dev.spaceapps.weather.core.settings.SettingsRepository
import dev.spaceapps.weather.feature.currentweather.domain.usecase.ObserveCurrentWeatherUseCase
import dev.spaceapps.weather.feature.currentweather.domain.usecase.RefreshCurrentWeatherUseCase
import dev.spaceapps.weather.feature.currentweather.domain.usecase.RefreshError
import dev.spaceapps.weather.feature.weather.presentation.WeatherError
import dev.spaceapps.weather.feature.weather.presentation.toUi
import dev.spaceapps.weather.feature.weather.presentation.toWeatherError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

class CurrentWeatherViewModel(
    observeCurrentWeather: ObserveCurrentWeatherUseCase,
    private val refreshCurrentWeather: RefreshCurrentWeatherUseCase,
    private val locationProvider: LocationProvider,
    settingsRepository: SettingsRepository,
    private val deviceTimeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : MviViewModel<CurrentWeatherState, CurrentWeatherIntent, CurrentWeatherEffect>(CurrentWeatherState()) {

    private var started = false
    private var refreshJob: Job? = null

    init {
        combine(observeCurrentWeather(), settingsRepository.settings) { weather, settings ->
            setState {
                copy(
                    weather = weather?.toUi(settings.unitSystem, deviceTimeZone()),
                    // Cached data makes the empty-state error obsolete.
                    error = if (weather != null) null else error,
                )
            }
        }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: CurrentWeatherIntent) {
        when (intent) {
            CurrentWeatherIntent.ScreenStarted -> onScreenStarted()
            is CurrentWeatherIntent.PermissionResult -> onPermissionResult(intent.granted)
            CurrentWeatherIntent.GrantPermissionClicked ->
                sendEffect(CurrentWeatherEffect.RequestLocationPermission)
            CurrentWeatherIntent.Refresh -> refresh()
        }
    }

    private fun onScreenStarted() {
        if (started) return
        started = true
        if (locationProvider.hasLocationPermission()) {
            refresh()
        } else {
            sendEffect(CurrentWeatherEffect.RequestLocationPermission)
        }
    }

    private fun onPermissionResult(granted: Boolean) {
        setState { copy(permission = granted.toPermission()) }
        // Even when denied, refresh: the use case falls back to the last known coordinates.
        refresh()
    }

    private fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            // Re-check on every refresh: the user may have changed it in system settings.
            setState {
                copy(isFetching = true, permission = locationProvider.hasLocationPermission().toPermission())
            }
            val result = refreshCurrentWeather()
            setState { copy(isFetching = false) }

            when (result) {
                is Outcome.Success -> setState { copy(error = null) }
                is Outcome.Failure -> {
                    val error = result.error.toWeatherError()
                    if (currentState.weather == null) {
                        setState { copy(error = error) }
                    } else {
                        sendEffect(CurrentWeatherEffect.ShowError(error))
                    }
                }
            }
        }
    }
}

private fun Boolean.toPermission(): LocationPermission =
    if (this) LocationPermission.GRANTED else LocationPermission.DENIED

private fun RefreshError.toWeatherError(): WeatherError = when (this) {
    is RefreshError.Location -> error.toWeatherError()
    is RefreshError.Remote -> error.toWeatherError()
}
