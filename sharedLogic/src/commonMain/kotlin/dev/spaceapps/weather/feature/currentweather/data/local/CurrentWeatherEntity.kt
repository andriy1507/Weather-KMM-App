package dev.spaceapps.weather.feature.currentweather.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Last fetched weather for the device location. Only one row is kept ([CURRENT_LOCATION_ID]).
 * Values are stored in metric units; conversion happens in the presentation layer.
 * Times are epoch seconds (UTC) as returned by the API, except [fetchedAtMillis].
 */
@Entity(tableName = "current_weather")
data class CurrentWeatherEntity(
    @PrimaryKey val id: Int = CURRENT_LOCATION_ID,
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
) {
    companion object {
        const val CURRENT_LOCATION_ID = 0
    }
}
