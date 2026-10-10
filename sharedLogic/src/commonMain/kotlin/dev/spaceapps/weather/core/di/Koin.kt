package dev.spaceapps.weather.core.di

import dev.spaceapps.weather.feature.cities.di.citiesModule
import dev.spaceapps.weather.feature.currentweather.di.currentWeatherModule
import dev.spaceapps.weather.feature.settings.di.settingsModule
import dev.spaceapps.weather.feature.weather.di.weatherModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication = startKoin {
    appDeclaration()
    modules(
        platformModule,
        coreModule,
        weatherModule,
        currentWeatherModule,
        citiesModule,
        settingsModule,
    )
}
