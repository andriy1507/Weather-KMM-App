package dev.spaceapps.weather.core.settings

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setUnitSystem(unitSystem: UnitSystem)
}
