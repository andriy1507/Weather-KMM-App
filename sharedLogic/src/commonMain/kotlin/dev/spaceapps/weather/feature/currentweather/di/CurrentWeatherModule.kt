package dev.spaceapps.weather.feature.currentweather.di

import dev.spaceapps.weather.feature.currentweather.data.CurrentWeatherRepositoryImpl
import dev.spaceapps.weather.feature.currentweather.data.remote.KtorOpenWeatherApi
import dev.spaceapps.weather.feature.currentweather.data.remote.OpenWeatherApi
import dev.spaceapps.weather.feature.currentweather.domain.CurrentWeatherRepository
import dev.spaceapps.weather.feature.currentweather.domain.usecase.ObserveCurrentWeatherUseCase
import dev.spaceapps.weather.feature.currentweather.domain.usecase.RefreshCurrentWeatherUseCase
import dev.spaceapps.weather.feature.currentweather.presentation.CurrentWeatherViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val currentWeatherModule = module {
    single<OpenWeatherApi> { KtorOpenWeatherApi(client = get()) }
    single<CurrentWeatherRepository> {
        CurrentWeatherRepositoryImpl(api = get(), dao = get(), clock = get())
    }

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
