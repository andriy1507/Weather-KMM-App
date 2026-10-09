package dev.spaceapps.weather.core.settings

import app.cash.turbine.test
import dev.spaceapps.weather.testing.FakePreferencesDataStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class DataStoreSettingsRepositoryTest {

    private val repository = DataStoreSettingsRepository(FakePreferencesDataStore())

    @Test
    fun `defaults to system theme and metric units`() = runTest {
        repository.settings.test {
            assertEquals(AppSettings(ThemeMode.SYSTEM, UnitSystem.METRIC), awaitItem())
        }
    }

    @Test
    fun `persists theme and unit changes`() = runTest {
        repository.settings.test {
            awaitItem()

            repository.setThemeMode(ThemeMode.DARK)
            assertEquals(AppSettings(ThemeMode.DARK, UnitSystem.METRIC), awaitItem())

            repository.setUnitSystem(UnitSystem.IMPERIAL)
            assertEquals(AppSettings(ThemeMode.DARK, UnitSystem.IMPERIAL), awaitItem())
        }
    }

    @Test
    fun `writing the same value does not emit again`() = runTest {
        repository.settings.test {
            awaitItem()

            repository.setThemeMode(ThemeMode.SYSTEM)

            expectNoEvents()
        }
    }
}
