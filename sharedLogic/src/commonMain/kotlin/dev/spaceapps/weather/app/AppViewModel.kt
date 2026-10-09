package dev.spaceapps.weather.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.spaceapps.weather.core.settings.SettingsRepository
import dev.spaceapps.weather.core.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** App-wide state for the root of the UI (theme). */
class AppViewModel(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.settings
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
}
