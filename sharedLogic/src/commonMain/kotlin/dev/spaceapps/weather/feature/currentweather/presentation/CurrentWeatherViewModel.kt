package dev.spaceapps.weather.feature.currentweather.presentation

import androidx.lifecycle.viewModelScope
import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.domain.Outcome
import dev.spaceapps.weather.core.location.LocationError
import dev.spaceapps.weather.core.location.LocationProvider
import dev.spaceapps.weather.core.mvi.MviViewModel
import dev.spaceapps.weather.core.settings.SettingsRepository
import dev.spaceapps.weather.feature.currentweather.domain.usecase.ObserveCurrentWeatherUseCase
import dev.spaceapps.weather.feature.currentweather.domain.usecase.RefreshCurrentWeatherUseCase
import dev.spaceapps.weather.feature.currentweather.domain.usecase.RefreshError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

class CurrentWeatherViewModel(
    observeCurrentWeather: ObserveCurrentWeatherUseCase,
    private val refreshCurrentWeather: RefreshCurrentWeatherUseCase,
    private val locationProvider: LocationProvider,
    private val settingsRepository: SettingsRepository,
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
                    unitSystem = settings.unitSystem,
                    themeMode = settings.themeMode,
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
            is CurrentWeatherIntent.ChangeUnitSystem ->
                viewModelScope.launch { settingsRepository.setUnitSystem(intent.unitSystem) }
            is CurrentWeatherIntent.ChangeThemeMode ->
                viewModelScope.launch { settingsRepository.setThemeMode(intent.themeMode) }
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
                    val error = result.error.toUiError()
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

private fun RefreshError.toUiError(): CurrentWeatherError = when (this) {
    is RefreshError.Location -> when (error) {
        LocationError.PERMISSION_DENIED -> CurrentWeatherError.LOCATION_PERMISSION_DENIED
        LocationError.LOCATION_DISABLED -> CurrentWeatherError.LOCATION_DISABLED
        LocationError.UNAVAILABLE -> CurrentWeatherError.LOCATION_UNAVAILABLE
    }
    is RefreshError.Remote -> when (error) {
        DataError.Remote.NO_INTERNET -> CurrentWeatherError.NO_INTERNET
        DataError.Remote.REQUEST_TIMEOUT -> CurrentWeatherError.TIMEOUT
        DataError.Remote.UNAUTHORIZED -> CurrentWeatherError.INVALID_API_KEY
        DataError.Remote.TOO_MANY_REQUESTS -> CurrentWeatherError.RATE_LIMITED
        DataError.Remote.SERVER -> CurrentWeatherError.SERVER
        DataError.Remote.SERIALIZATION, DataError.Remote.UNKNOWN -> CurrentWeatherError.UNKNOWN
    }
}
