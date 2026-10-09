package dev.spaceapps.weather.core.di

import dev.spaceapps.weather.core.database.appDatabaseBuilder
import dev.spaceapps.weather.core.datastore.createDataStore
import dev.spaceapps.weather.core.location.AndroidLocationProvider
import dev.spaceapps.weather.core.location.LocationProvider
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    single<HttpClientEngine> { OkHttp.create() }
    single { appDatabaseBuilder(androidContext()) }
    single { createDataStore(androidContext()) }
    single<LocationProvider> { AndroidLocationProvider(androidContext()) }
}
