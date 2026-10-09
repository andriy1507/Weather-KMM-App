package dev.spaceapps.weather.core.di

import dev.spaceapps.weather.BuildKonfig
import dev.spaceapps.weather.app.AppViewModel
import dev.spaceapps.weather.core.database.AppDatabase
import dev.spaceapps.weather.core.database.createAppDatabase
import dev.spaceapps.weather.core.network.createHttpClient
import dev.spaceapps.weather.core.network.createJson
import dev.spaceapps.weather.core.settings.DataStoreSettingsRepository
import dev.spaceapps.weather.core.settings.SettingsRepository
import kotlin.time.Clock
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Platform specifics required by [coreModule]:
 * - `HttpClientEngine`
 * - `RoomDatabase.Builder<AppDatabase>`
 * - `DataStore<Preferences>`
 * - `LocationProvider`
 */
internal expect val platformModule: Module

internal val coreModule = module {
    single<Clock> { Clock.System }

    single { createJson() }
    single { createHttpClient(engine = get(), json = get(), apiKey = BuildKonfig.OPEN_WEATHER_API_KEY) }

    single { createAppDatabase(builder = get()) }
    single { get<AppDatabase>().currentWeatherDao() }

    single<SettingsRepository> { DataStoreSettingsRepository(dataStore = get()) }

    viewModel { AppViewModel(settingsRepository = get()) }
}
