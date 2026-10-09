package dev.spaceapps.weather.feature.currentweather.data

import dev.spaceapps.weather.core.network.createJson
import dev.spaceapps.weather.feature.currentweather.data.mapper.toDomain
import dev.spaceapps.weather.feature.currentweather.data.mapper.toEntity
import dev.spaceapps.weather.feature.currentweather.data.remote.CurrentWeatherDto
import dev.spaceapps.weather.core.location.Coordinates
import dev.spaceapps.weather.testing.CURRENT_WEATHER_JSON
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant
import kotlinx.datetime.UtcOffset
import kotlinx.serialization.SerializationException

class CurrentWeatherMappersTest {

    private val dto = createJson().decodeFromString<CurrentWeatherDto>(CURRENT_WEATHER_JSON)
    private val fetchedAt = Instant.fromEpochMilliseconds(1_791_540_123_456)

    @Test
    fun `dto maps through entity to domain`() {
        val weather = dto.toEntity(fetchedAt).toDomain()

        assertEquals(Coordinates(latitude = 50.4501, longitude = 30.5234), weather.coordinates)
        assertEquals("Kyiv", weather.locationName)
        assertEquals("UA", weather.countryCode)
        assertEquals(803, weather.condition.id)
        assertEquals("broken clouds", weather.condition.description)
        assertEquals("04d", weather.condition.iconCode)
        assertEquals(12.34, weather.temperatureCelsius)
        assertEquals(11.2, weather.feelsLikeCelsius)
        assertEquals(10.5, weather.minTemperatureCelsius)
        assertEquals(13.9, weather.maxTemperatureCelsius)
        assertEquals(1015, weather.pressureHpa)
        assertEquals(71, weather.humidityPercent)
        assertEquals(10000, weather.visibilityMeters)
        assertEquals(4.12, weather.windSpeedMps)
        assertEquals(240, weather.windDirectionDegrees)
        assertEquals(75, weather.cloudinessPercent)
        assertEquals(Instant.fromEpochSeconds(1_791_518_400), weather.sunrise)
        assertEquals(Instant.fromEpochSeconds(1_791_558_000), weather.sunset)
        assertEquals(UtcOffset(hours = 3), weather.utcOffset)
        assertEquals(Instant.fromEpochSeconds(1_791_540_000), weather.observedAt)
        assertEquals(fetchedAt, weather.fetchedAt)
    }

    @Test
    fun `optional fields missing from response map to null or defaults`() {
        val minimal = dto.copy(visibility = null, sys = dto.sys.copy(country = null))

        val weather = minimal.toEntity(fetchedAt).toDomain()

        assertEquals(null, weather.visibilityMeters)
        assertEquals(null, weather.countryCode)
    }

    @Test
    fun `response without weather condition is rejected`() {
        assertFailsWith<SerializationException> {
            dto.copy(weather = emptyList()).toEntity(fetchedAt)
        }
    }
}
