package dev.spaceapps.weather.feature.currentweather.data.mapper

import dev.spaceapps.weather.feature.currentweather.data.local.CurrentWeatherEntity
import dev.spaceapps.weather.feature.currentweather.data.remote.CurrentWeatherDto
import dev.spaceapps.weather.core.location.Coordinates
import dev.spaceapps.weather.feature.currentweather.domain.model.CurrentWeather
import dev.spaceapps.weather.feature.currentweather.domain.model.WeatherCondition
import kotlin.time.Instant
import kotlinx.datetime.UtcOffset
import kotlinx.serialization.SerializationException

/** @throws SerializationException if the response has no weather condition. */
fun CurrentWeatherDto.toEntity(fetchedAt: Instant): CurrentWeatherEntity {
    // The API returns a list; the first entry is the primary condition.
    val condition = weather.firstOrNull()
        ?: throw SerializationException("Current weather response has no weather condition")
    return CurrentWeatherEntity(
        latitude = coord.lat,
        longitude = coord.lon,
        locationName = name,
        countryCode = sys.country,
        conditionId = condition.id,
        conditionMain = condition.main,
        conditionDescription = condition.description,
        iconCode = condition.icon,
        temperatureCelsius = main.temp,
        feelsLikeCelsius = main.feelsLike,
        minTemperatureCelsius = main.tempMin,
        maxTemperatureCelsius = main.tempMax,
        pressureHpa = main.pressure,
        humidityPercent = main.humidity,
        visibilityMeters = visibility,
        windSpeedMps = wind.speed,
        windDirectionDegrees = wind.deg,
        cloudinessPercent = clouds.all,
        sunriseEpochSeconds = sys.sunrise,
        sunsetEpochSeconds = sys.sunset,
        timezoneOffsetSeconds = timezone,
        observedAtEpochSeconds = dt,
        fetchedAtMillis = fetchedAt.toEpochMilliseconds(),
    )
}

fun CurrentWeatherEntity.toDomain(): CurrentWeather = CurrentWeather(
    coordinates = toCoordinates(),
    locationName = locationName,
    countryCode = countryCode,
    condition = WeatherCondition(
        id = conditionId,
        main = conditionMain,
        description = conditionDescription,
        iconCode = iconCode,
    ),
    temperatureCelsius = temperatureCelsius,
    feelsLikeCelsius = feelsLikeCelsius,
    minTemperatureCelsius = minTemperatureCelsius,
    maxTemperatureCelsius = maxTemperatureCelsius,
    pressureHpa = pressureHpa,
    humidityPercent = humidityPercent,
    visibilityMeters = visibilityMeters,
    windSpeedMps = windSpeedMps,
    windDirectionDegrees = windDirectionDegrees,
    cloudinessPercent = cloudinessPercent,
    sunrise = Instant.fromEpochSeconds(sunriseEpochSeconds),
    sunset = Instant.fromEpochSeconds(sunsetEpochSeconds),
    utcOffset = UtcOffset(seconds = timezoneOffsetSeconds),
    observedAt = Instant.fromEpochSeconds(observedAtEpochSeconds),
    fetchedAt = Instant.fromEpochMilliseconds(fetchedAtMillis),
)

fun CurrentWeatherEntity.toCoordinates(): Coordinates = Coordinates(latitude, longitude)
