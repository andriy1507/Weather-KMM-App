package dev.spaceapps.weather.core.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class UnitSystem { METRIC, IMPERIAL }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
)
