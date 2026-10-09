package dev.spaceapps.weather.feature.currentweather.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

interface OpenWeatherApi {
    suspend fun getCurrentWeather(latitude: Double, longitude: Double): CurrentWeatherDto
}

class KtorOpenWeatherApi(
    private val client: HttpClient,
) : OpenWeatherApi {

    override suspend fun getCurrentWeather(latitude: Double, longitude: Double): CurrentWeatherDto =
        client.get(CURRENT_WEATHER_PATH) {
            parameter("lat", latitude)
            parameter("lon", longitude)
            // Always metric: the cache stays unit-agnostic and conversion happens in the UI layer.
            parameter("units", "metric")
        }.body()

    private companion object {
        const val CURRENT_WEATHER_PATH = "data/2.5/weather"
    }
}
