package dev.spaceapps.weather.feature.weather.presentation

import dev.spaceapps.weather.core.settings.UnitSystem
import dev.spaceapps.weather.testing.sampleCurrentWeather
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.TimeZone

class CurrentWeatherUiTest {

    private val weather = sampleCurrentWeather()

    @Test
    fun `formats metric values`() {
        val ui = weather.toUi(UnitSystem.METRIC, TimeZone.UTC)

        assertEquals("Kyiv, UA", ui.location)
        assertEquals("Broken clouds", ui.conditionText)
        assertEquals(803, ui.conditionId)
        assertEquals("https://openweathermap.org/img/wn/04d@4x.png", ui.iconUrl)
        assertEquals("12°", ui.temperature)
        assertEquals("11°", ui.feelsLike)
        assertEquals("11°", ui.minTemperature)
        assertEquals("14°", ui.maxTemperature)
        assertEquals("71%", ui.humidity)
        assertEquals("1015 hPa", ui.pressure)
        assertEquals("4.1 m/s", ui.wind)
        assertEquals(240, ui.windDirectionDegrees)
        assertEquals("10 km", ui.visibility)
        assertEquals("75%", ui.cloudiness)
    }

    @Test
    fun `formats imperial values`() {
        val ui = weather.toUi(UnitSystem.IMPERIAL, TimeZone.UTC)

        assertEquals("54°", ui.temperature)
        assertEquals("52°", ui.feelsLike)
        assertEquals("51°", ui.minTemperature)
        assertEquals("57°", ui.maxTemperature)
        assertEquals("9.2 mph", ui.wind)
        assertEquals("6.2 mi", ui.visibility)
        assertEquals("1015 hPa", ui.pressure)
    }

    @Test
    fun `sun times use the location offset while update time uses the device zone`() {
        val ui = weather.toUi(UnitSystem.METRIC, TimeZone.of("America/New_York"))

        // Sunrise 04:00 UTC, sunset 15:00 UTC, location is UTC+3.
        assertEquals("07:00", ui.sunrise)
        assertEquals("18:00", ui.sunset)
        // Fetched 10:02 UTC; New York is UTC-4 on that date (DST).
        assertEquals("06:02", ui.updatedAt)
    }

    @Test
    fun `location omits missing parts and missing visibility stays null`() {
        val ui = weather.copy(locationName = "", visibilityMeters = null)
            .toUi(UnitSystem.METRIC, TimeZone.UTC)

        assertEquals("UA", ui.location)
        assertEquals(null, ui.visibility)
    }

    @Test
    fun `temperatures round to whole degrees including below zero`() {
        val ui = weather.copy(temperatureCelsius = -0.4, feelsLikeCelsius = -5.6)
            .toUi(UnitSystem.METRIC, TimeZone.UTC)

        assertEquals("0°", ui.temperature)
        assertEquals("-6°", ui.feelsLike)
    }
}
