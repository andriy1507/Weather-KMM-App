package dev.spaceapps.weather.core.di

import dev.spaceapps.weather.core.database.appDatabaseBuilder
import dev.spaceapps.weather.core.datastore.createDataStore
import dev.spaceapps.weather.core.location.IosLocationProvider
import dev.spaceapps.weather.core.location.LocationProvider
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    single<HttpClientEngine> { Darwin.create() }
    single { appDatabaseBuilder() }
    single { createDataStore() }
    single<LocationProvider> { IosLocationProvider() }
}
