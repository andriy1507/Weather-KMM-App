package dev.spaceapps.weather.feature.currentweather.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of `GET /data/2.5/weather` with `units=metric`. */
@Serializable
data class CurrentWeatherDto(
    val coord: CoordinatesDto,
    val weather: List<WeatherConditionDto>,
    val main: MainDto,
    val visibility: Int? = null,
    val wind: WindDto,
    val clouds: CloudsDto,
    val dt: Long,
    val sys: SysDto,
    val timezone: Int,
    val name: String,
)

@Serializable
data class CoordinatesDto(
    val lat: Double,
    val lon: Double,
)

@Serializable
data class WeatherConditionDto(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String,
)

@Serializable
data class MainDto(
    val temp: Double,
    @SerialName("feels_like") val feelsLike: Double,
    @SerialName("temp_min") val tempMin: Double,
    @SerialName("temp_max") val tempMax: Double,
    val pressure: Int,
    val humidity: Int,
)

@Serializable
data class WindDto(
    val speed: Double,
    val deg: Int = 0,
)

@Serializable
data class CloudsDto(
    val all: Int,
)

@Serializable
data class SysDto(
    val country: String? = null,
    val sunrise: Long,
    val sunset: Long,
)
