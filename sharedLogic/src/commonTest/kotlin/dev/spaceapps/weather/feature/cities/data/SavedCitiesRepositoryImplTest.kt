package dev.spaceapps.weather.feature.cities.data

import app.cash.turbine.test
import dev.spaceapps.weather.core.location.Coordinates
import dev.spaceapps.weather.feature.cities.domain.model.CityCandidate
import dev.spaceapps.weather.testing.FakeSavedCityDao
import dev.spaceapps.weather.testing.FixedClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class SavedCitiesRepositoryImplTest {

    private val dao = FakeSavedCityDao()
    private val clock = FixedClock(Instant.fromEpochMilliseconds(1_000))
    private val repository = SavedCitiesRepositoryImpl(dao, clock)

    private val lviv = CityCandidate("Lviv", null, "UA", Coordinates(49.8397166, 24.0297162))
    private val odesa = CityCandidate("Odesa", null, "UA", Coordinates(46.4825, 30.7233))
    private val springfield = CityCandidate("Springfield", "Illinois", "US", Coordinates(39.7990, -89.6440))

    @Test
    fun `add appends cities in order with rounded coordinates`() = runTest {
        val first = repository.add(lviv)
        val second = repository.add(odesa)

        assertEquals(Coordinates(49.8397, 24.0297), first.coordinates)
        assertEquals(1_000, first.addedAtMillis)
        assertEquals(0, first.position)
        assertEquals(1, second.position)
        repository.observeCities().test {
            assertEquals(listOf("Lviv", "Odesa"), awaitItem().map { it.name })
        }
    }

    @Test
    fun `adding an already saved place returns the existing city`() = runTest {
        val saved = repository.add(lviv)

        val again = repository.add(lviv.copy(name = "Lemberg", coordinates = Coordinates(49.83972, 24.02971)))

        assertEquals(saved, again)
        assertEquals(1, dao.rows.value.size)
    }

    @Test
    fun `remove deletes the city and its cached weather`() = runTest {
        val saved = repository.add(springfield)

        val removed = repository.remove(saved.id)

        assertEquals(saved, removed)
        assertEquals(emptyList(), dao.rows.value)
        assertEquals(listOf("city:${saved.id}"), dao.deletedWeatherKeys)
    }

    @Test
    fun `removing an unknown city returns null`() = runTest {
        assertNull(repository.remove(42))
    }

    @Test
    fun `restore puts the city back with its id and position`() = runTest {
        repository.add(lviv)
        val middle = repository.add(odesa)
        repository.add(springfield)

        repository.restore(checkNotNull(repository.remove(middle.id)))

        assertEquals(middle, repository.getCity(middle.id))
        repository.observeCities().test {
            assertEquals(listOf("Lviv", "Odesa", "Springfield"), awaitItem().map { it.name })
        }
    }
}
