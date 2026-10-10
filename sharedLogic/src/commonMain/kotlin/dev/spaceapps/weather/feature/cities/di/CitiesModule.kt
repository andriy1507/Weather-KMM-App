package dev.spaceapps.weather.feature.cities.di

import dev.spaceapps.weather.feature.cities.data.CitySearchRepositoryImpl
import dev.spaceapps.weather.feature.cities.data.SavedCitiesRepositoryImpl
import dev.spaceapps.weather.feature.cities.data.remote.GeocodingApi
import dev.spaceapps.weather.feature.cities.data.remote.KtorGeocodingApi
import dev.spaceapps.weather.feature.cities.domain.CitySearchRepository
import dev.spaceapps.weather.feature.cities.domain.SavedCitiesRepository
import dev.spaceapps.weather.feature.cities.presentation.detail.CityWeatherViewModel
import dev.spaceapps.weather.feature.cities.presentation.list.CitiesViewModel
import dev.spaceapps.weather.feature.cities.presentation.search.CitySearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val citiesModule = module {
    single<GeocodingApi> { KtorGeocodingApi(client = get()) }
    single<SavedCitiesRepository> { SavedCitiesRepositoryImpl(dao = get(), clock = get()) }
    single<CitySearchRepository> { CitySearchRepositoryImpl(api = get(), dao = get()) }

    viewModel {
        CitiesViewModel(citiesRepository = get(), weatherRepository = get(), settingsRepository = get())
    }
    viewModel { CitySearchViewModel(searchRepository = get(), citiesRepository = get()) }
    // Resolve with parametersOf(cityId).
    viewModel { params ->
        CityWeatherViewModel(
            cityId = params.get(),
            citiesRepository = get(),
            weatherRepository = get(),
            settingsRepository = get(),
        )
    }
}
