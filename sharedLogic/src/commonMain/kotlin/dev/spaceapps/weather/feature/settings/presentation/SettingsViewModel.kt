package dev.spaceapps.weather.feature.settings.presentation

import androidx.lifecycle.viewModelScope
import dev.spaceapps.weather.core.mvi.MviViewModel
import dev.spaceapps.weather.core.settings.SettingsRepository
import dev.spaceapps.weather.core.settings.ThemeMode
import dev.spaceapps.weather.core.settings.UnitSystem
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SettingsState(
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

sealed interface SettingsIntent {
    data class ChangeUnitSystem(val unitSystem: UnitSystem) : SettingsIntent
    data class ChangeThemeMode(val themeMode: ThemeMode) : SettingsIntent
}

/** No one-off events; changes are reflected through [SettingsState]. */
sealed interface SettingsEffect

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : MviViewModel<SettingsState, SettingsIntent, SettingsEffect>(SettingsState()) {

    init {
        settingsRepository.settings
            .onEach { settings ->
                setState { copy(unitSystem = settings.unitSystem, themeMode = settings.themeMode) }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: SettingsIntent) {
        viewModelScope.launch {
            when (intent) {
                is SettingsIntent.ChangeUnitSystem -> settingsRepository.setUnitSystem(intent.unitSystem)
                is SettingsIntent.ChangeThemeMode -> settingsRepository.setThemeMode(intent.themeMode)
            }
        }
    }
}
