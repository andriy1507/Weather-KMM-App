package dev.spaceapps.weather.feature.currentweather.domain.usecase

import dev.spaceapps.weather.feature.currentweather.domain.CurrentWeatherRepository
import dev.spaceapps.weather.feature.currentweather.domain.model.CurrentWeather
import kotlinx.coroutines.flow.Flow

class ObserveCurrentWeatherUseCase(
    private val repository: CurrentWeatherRepository,
) {
    operator fun invoke(): Flow<CurrentWeather?> = repository.observeCurrentWeather()
}
