package dev.spaceapps.weather.feature.settings

import dev.spaceapps.weather.core.settings.AppSettings
import dev.spaceapps.weather.core.settings.DataStoreSettingsRepository
import dev.spaceapps.weather.core.settings.ThemeMode
import dev.spaceapps.weather.core.settings.UnitSystem
import dev.spaceapps.weather.feature.settings.presentation.SettingsIntent
import dev.spaceapps.weather.feature.settings.presentation.SettingsState
import dev.spaceapps.weather.feature.settings.presentation.SettingsViewModel
import dev.spaceapps.weather.testing.FakePreferencesDataStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class SettingsViewModelTest {

    private val settingsRepository = DataStoreSettingsRepository(FakePreferencesDataStore())

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `changes are saved and reflected in state`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository)
        assertEquals(SettingsState(UnitSystem.METRIC, ThemeMode.SYSTEM), viewModel.state.value)

        viewModel.onIntent(SettingsIntent.ChangeUnitSystem(UnitSystem.IMPERIAL))
        viewModel.onIntent(SettingsIntent.ChangeThemeMode(ThemeMode.DARK))

        assertEquals(SettingsState(UnitSystem.IMPERIAL, ThemeMode.DARK), viewModel.state.value)
        assertEquals(AppSettings(ThemeMode.DARK, UnitSystem.IMPERIAL), settingsRepository.settings.first())
    }
}
