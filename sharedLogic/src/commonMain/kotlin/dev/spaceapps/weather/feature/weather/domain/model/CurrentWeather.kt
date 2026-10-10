package dev.spaceapps.weather.feature.weather.domain.model

import dev.spaceapps.weather.core.location.Coordinates
import kotlin.time.Instant
import kotlinx.datetime.UtcOffset

data class WeatherCondition(
    /** OpenWeather condition code, see https://openweathermap.org/weather-conditions */
    val id: Int,
    val main: String,
    val description: String,
    val iconCode: String,
)

/** All measurements are metric. */
data class CurrentWeather(
    val coordinates: Coordinates,
    val locationName: String,
    val countryCode: String?,
    val condition: WeatherCondition,
    val temperatureCelsius: Double,
    val feelsLikeCelsius: Double,
    val minTemperatureCelsius: Double,
    val maxTemperatureCelsius: Double,
    val pressureHpa: Int,
    val humidityPercent: Int,
    val visibilityMeters: Int?,
    val windSpeedMps: Double,
    val windDirectionDegrees: Int,
    val cloudinessPercent: Int,
    val sunrise: Instant,
    val sunset: Instant,
    /** Offset of the weather location (not of the device). */
    val utcOffset: UtcOffset,
    val observedAt: Instant,
    val fetchedAt: Instant,
)
