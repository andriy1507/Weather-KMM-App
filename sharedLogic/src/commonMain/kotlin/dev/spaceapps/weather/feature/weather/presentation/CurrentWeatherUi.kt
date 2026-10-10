package dev.spaceapps.weather.feature.weather.presentation

import dev.spaceapps.weather.core.settings.UnitSystem
import dev.spaceapps.weather.feature.weather.domain.model.CurrentWeather
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

/**
 * Display-ready weather: values converted to the selected [UnitSystem] and formatted.
 * Field labels ("Humidity", ...) are left to the platform UI for localization.
 */
data class CurrentWeatherUi(
    val location: String,
    val conditionText: String,
    /** OpenWeather condition code, for condition-specific visuals. */
    val conditionId: Int,
    val iconUrl: String,
    val temperature: String,
    val feelsLike: String,
    val minTemperature: String,
    val maxTemperature: String,
    val humidity: String,
    val pressure: String,
    val wind: String,
    val windDirectionDegrees: Int,
    val visibility: String?,
    val cloudiness: String,
    /** Local time at the weather location. */
    val sunrise: String,
    val sunset: String,
    /** Device local time of the last successful fetch. */
    val updatedAt: String,
)

private const val MPS_TO_MPH = 2.236936
private const val METERS_PER_MILE = 1609.344

private val timeFormat = LocalTime.Format {
    hour()
    char(':')
    minute()
}

fun CurrentWeather.toUi(unitSystem: UnitSystem, deviceTimeZone: TimeZone): CurrentWeatherUi {
    val locationTimeZone = utcOffset.asTimeZone()
    return CurrentWeatherUi(
        location = listOfNotNull(locationName.takeIf { it.isNotBlank() }, countryCode).joinToString(", "),
        conditionText = condition.description.replaceFirstChar { it.uppercase() },
        conditionId = condition.id,
        iconUrl = "https://openweathermap.org/img/wn/${condition.iconCode}@4x.png",
        temperature = temperatureCelsius.formatTemperature(unitSystem),
        feelsLike = feelsLikeCelsius.formatTemperature(unitSystem),
        minTemperature = minTemperatureCelsius.formatTemperature(unitSystem),
        maxTemperature = maxTemperatureCelsius.formatTemperature(unitSystem),
        humidity = "$humidityPercent%",
        pressure = "$pressureHpa hPa",
        wind = when (unitSystem) {
            UnitSystem.METRIC -> "${windSpeedMps.formatOneDecimal()} m/s"
            UnitSystem.IMPERIAL -> "${(windSpeedMps * MPS_TO_MPH).formatOneDecimal()} mph"
        },
        windDirectionDegrees = windDirectionDegrees,
        visibility = visibilityMeters?.let { meters ->
            when (unitSystem) {
                UnitSystem.METRIC -> "${(meters / 1000.0).formatOneDecimal()} km"
                UnitSystem.IMPERIAL -> "${(meters / METERS_PER_MILE).formatOneDecimal()} mi"
            }
        },
        cloudiness = "$cloudinessPercent%",
        sunrise = sunrise.formatTime(locationTimeZone),
        sunset = sunset.formatTime(locationTimeZone),
        updatedAt = fetchedAt.formatTime(deviceTimeZone),
    )
}

private fun Double.formatTemperature(unitSystem: UnitSystem): String {
    val value = when (unitSystem) {
        UnitSystem.METRIC -> this
        UnitSystem.IMPERIAL -> this * 9 / 5 + 32
    }
    return "${value.roundToInt()}°"
}

/** Common code has no `String.format`; one decimal place, trailing ".0" dropped. */
private fun Double.formatOneDecimal(): String {
    val tenths = (this * 10).roundToInt()
    val sign = if (tenths < 0) "-" else ""
    val whole = abs(tenths) / 10
    val fraction = abs(tenths) % 10
    return if (fraction == 0) "$sign$whole" else "$sign$whole.$fraction"
}

private fun Instant.formatTime(timeZone: TimeZone): String =
    timeFormat.format(toLocalDateTime(timeZone).time)
