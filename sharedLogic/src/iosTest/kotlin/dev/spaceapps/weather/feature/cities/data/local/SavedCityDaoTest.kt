package dev.spaceapps.weather.feature.cities.data.local

import androidx.room3.Room
import dev.spaceapps.weather.core.database.AppDatabase
import dev.spaceapps.weather.core.database.AppDatabaseConstructor
import dev.spaceapps.weather.core.database.createAppDatabase
import dev.spaceapps.weather.feature.weather.data.local.WeatherCacheEntity
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/** Runs against real SQLite to cover what the fakes can't: the unique index and transactions. */
class SavedCityDaoTest {

    private val database: AppDatabase = createAppDatabase(
        Room.inMemoryDatabaseBuilder<AppDatabase>(factory = { AppDatabaseConstructor.initialize() }),
    )
    private val dao = database.savedCityDao()
    private val weatherDao = database.weatherCacheDao()

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    private fun city(name: String, latitude: Double, longitude: Double) = SavedCityEntity(
        name = name,
        state = null,
        countryCode = "UA",
        latitude = latitude,
        longitude = longitude,
        position = 0,
        addedAtMillis = 0,
    )

    @Test
    fun insertLastAssignsIncreasingPositions() = runTest {
        dao.insertLast(city("Lviv", 49.8397, 24.0297))
        dao.insertLast(city("Odesa", 46.4825, 30.7233))

        assertEquals(listOf("Lviv" to 0, "Odesa" to 1), dao.getAll().map { it.name to it.position })
    }

    @Test
    fun duplicateCoordinatesAreIgnored() = runTest {
        dao.insertLast(city("Lviv", 49.8397, 24.0297))

        val id = dao.insertLast(city("Lemberg", 49.8397, 24.0297))

        assertEquals(-1L, id)
        assertEquals(listOf("Lviv"), dao.getAll().map { it.name })
    }

    @Test
    fun deleteWithWeatherRemovesCachedWeather() = runTest {
        val id = dao.insertLast(city("Lviv", 49.8397, 24.0297))
        weatherDao.upsert(weather("city:$id"))
        weatherDao.upsert(weather("current"))

        assertTrue(dao.deleteWithWeather(id, "city:$id"))

        assertNull(dao.get(id))
        assertNull(weatherDao.get("city:$id"))
        assertEquals("current", weatherDao.get("current")?.locationKey)
        assertFalse(dao.deleteWithWeather(id, "city:$id"))
    }

    private fun weather(locationKey: String) = WeatherCacheEntity(
        locationKey = locationKey,
        latitude = 0.0,
        longitude = 0.0,
        locationName = "",
        countryCode = null,
        conditionId = 800,
        conditionMain = "Clear",
        conditionDescription = "clear sky",
        iconCode = "01d",
        temperatureCelsius = 0.0,
        feelsLikeCelsius = 0.0,
        minTemperatureCelsius = 0.0,
        maxTemperatureCelsius = 0.0,
        pressureHpa = 0,
        humidityPercent = 0,
        visibilityMeters = null,
        windSpeedMps = 0.0,
        windDirectionDegrees = 0,
        cloudinessPercent = 0,
        sunriseEpochSeconds = 0,
        sunsetEpochSeconds = 0,
        timezoneOffsetSeconds = 0,
        observedAtEpochSeconds = 0,
        fetchedAtMillis = 0,
    )
}
