package dev.spaceapps.weather.feature.currentweather.di

import dev.spaceapps.weather.feature.currentweather.domain.usecase.ObserveCurrentWeatherUseCase
import dev.spaceapps.weather.feature.currentweather.domain.usecase.RefreshCurrentWeatherUseCase
import dev.spaceapps.weather.feature.currentweather.presentation.CurrentWeatherViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val currentWeatherModule = module {
    factory { ObserveCurrentWeatherUseCase(repository = get()) }
    factory { RefreshCurrentWeatherUseCase(locationProvider = get(), repository = get()) }

    viewModel {
        CurrentWeatherViewModel(
            observeCurrentWeather = get(),
            refreshCurrentWeather = get(),
            locationProvider = get(),
            settingsRepository = get(),
        )
    }
}
