package dev.spaceapps.weather.feature.settings.di

import dev.spaceapps.weather.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val settingsModule = module {
    viewModel { SettingsViewModel(settingsRepository = get()) }
}
