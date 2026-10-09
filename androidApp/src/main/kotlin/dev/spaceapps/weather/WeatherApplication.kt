package dev.spaceapps.weather

import android.app.Application
import dev.spaceapps.weather.core.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class WeatherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@WeatherApplication)
        }
    }
}
