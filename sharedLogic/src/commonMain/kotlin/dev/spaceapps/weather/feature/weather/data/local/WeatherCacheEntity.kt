package dev.spaceapps.weather.feature.weather.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Last fetched weather for one [location][dev.spaceapps.weather.feature.weather.domain.WeatherLocation],
 * keyed by its `cacheKey`. Values are stored in metric units; conversion happens in the presentation layer.
 * Times are epoch seconds (UTC) as returned by the API, except [fetchedAtMillis].
 */
@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val locationKey: String,
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val countryCode: String?,
    val conditionId: Int,
    val conditionMain: String,
    val conditionDescription: String,
    val iconCode: String,
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
    val sunriseEpochSeconds: Long,
    val sunsetEpochSeconds: Long,
    val timezoneOffsetSeconds: Int,
    val observedAtEpochSeconds: Long,
    val fetchedAtMillis: Long,
)
