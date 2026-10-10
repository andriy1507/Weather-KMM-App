package dev.spaceapps.weather.feature.cities.data

import dev.spaceapps.weather.feature.cities.data.local.SavedCityDao
import dev.spaceapps.weather.feature.cities.domain.SavedCitiesRepository
import dev.spaceapps.weather.feature.cities.domain.model.CityCandidate
import dev.spaceapps.weather.feature.cities.domain.model.SavedCity
import dev.spaceapps.weather.feature.weather.domain.WeatherLocation
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavedCitiesRepositoryImpl(
    private val dao: SavedCityDao,
    private val clock: Clock,
) : SavedCitiesRepository {

    override fun observeCities(): Flow<List<SavedCity>> =
        dao.observeAll().map { cities -> cities.map { it.toDomain() } }

    override suspend fun getCity(id: Long): SavedCity? = dao.get(id)?.toDomain()

    override suspend fun add(candidate: CityCandidate): SavedCity {
        val entity = candidate.toNewEntity(addedAtMillis = clock.now().toEpochMilliseconds())
        val id = dao.insertLast(entity)
        val saved = if (id == -1L) {
            dao.findByCoordinates(entity.latitude, entity.longitude)
        } else {
            dao.get(id)
        }
        return checkNotNull(saved) { "Saved city not found after insert" }.toDomain()
    }

    override suspend fun remove(id: Long): SavedCity? {
        val city = dao.get(id) ?: return null
        dao.deleteWithWeather(id, WeatherLocation.SavedCity(id).cacheKey)
        return city.toDomain()
    }

    override suspend fun restore(city: SavedCity) {
        // Same id and position as before; ignored if the place was saved again in the meantime.
        dao.insert(city.toEntity())
    }
}
