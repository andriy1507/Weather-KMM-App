package dev.spaceapps.weather.feature.currentweather.domain.usecase

import dev.spaceapps.weather.feature.weather.domain.WeatherLocation
import dev.spaceapps.weather.feature.weather.domain.WeatherRepository
import dev.spaceapps.weather.feature.weather.domain.model.CurrentWeather
import kotlinx.coroutines.flow.Flow

class ObserveCurrentWeatherUseCase(
    private val repository: WeatherRepository,
) {
    operator fun invoke(): Flow<CurrentWeather?> = repository.observeWeather(WeatherLocation.Current)
}
