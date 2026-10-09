package dev.spaceapps.weather.testing

import dev.spaceapps.weather.core.network.createJson
import dev.spaceapps.weather.feature.currentweather.data.mapper.toDomain
import dev.spaceapps.weather.feature.currentweather.data.mapper.toEntity
import dev.spaceapps.weather.feature.currentweather.data.remote.CurrentWeatherDto
import dev.spaceapps.weather.feature.currentweather.domain.model.CurrentWeather
import kotlin.time.Instant

/** Real response shape of `GET /data/2.5/weather?units=metric` (Kyiv). */
const val CURRENT_WEATHER_JSON = """
{
  "coord": { "lon": 30.5234, "lat": 50.4501 },
  "weather": [
    { "id": 803, "main": "Clouds", "description": "broken clouds", "icon": "04d" },
    { "id": 500, "main": "Rain", "description": "light rain", "icon": "10d" }
  ],
  "base": "stations",
  "main": {
    "temp": 12.34, "feels_like": 11.2, "temp_min": 10.5, "temp_max": 13.9,
    "pressure": 1015, "humidity": 71, "sea_level": 1015, "grnd_level": 996
  },
  "visibility": 10000,
  "wind": { "speed": 4.12, "deg": 240, "gust": 7.5 },
  "clouds": { "all": 75 },
  "dt": 1791540000,
  "sys": { "type": 2, "id": 2003742, "country": "UA", "sunrise": 1791518400, "sunset": 1791558000 },
  "timezone": 10800,
  "id": 703448,
  "name": "Kyiv",
  "cod": 200
}
"""

/** [CURRENT_WEATHER_JSON] as a domain model; fetched at 10:02:03 UTC. */
fun sampleCurrentWeather(): CurrentWeather =
    createJson().decodeFromString<CurrentWeatherDto>(CURRENT_WEATHER_JSON)
        .toEntity(fetchedAt = Instant.fromEpochMilliseconds(1_791_540_123_456))
        .toDomain()
