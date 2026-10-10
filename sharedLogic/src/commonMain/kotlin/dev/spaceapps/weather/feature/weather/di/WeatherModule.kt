package dev.spaceapps.weather.feature.weather.di

import dev.spaceapps.weather.feature.weather.data.WeatherRepositoryImpl
import dev.spaceapps.weather.feature.weather.data.remote.KtorOpenWeatherApi
import dev.spaceapps.weather.feature.weather.data.remote.OpenWeatherApi
import dev.spaceapps.weather.feature.weather.domain.WeatherRepository
import org.koin.dsl.module

internal val weatherModule = module {
    single<OpenWeatherApi> { KtorOpenWeatherApi(client = get()) }
    single<WeatherRepository> { WeatherRepositoryImpl(api = get(), dao = get(), clock = get()) }
}
