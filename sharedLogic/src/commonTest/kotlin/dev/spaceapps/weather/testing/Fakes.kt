package dev.spaceapps.weather.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import dev.spaceapps.weather.feature.cities.data.local.SavedCityDao
import dev.spaceapps.weather.feature.cities.data.local.SavedCityEntity
import dev.spaceapps.weather.feature.weather.data.local.WeatherCacheDao
import dev.spaceapps.weather.feature.weather.data.local.WeatherCacheEntity
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FixedClock(var now: Instant) : Clock {
    override fun now(): Instant = now
}

class FakeWeatherCacheDao : WeatherCacheDao {
    val rows = MutableStateFlow<Map<String, WeatherCacheEntity>>(emptyMap())

    override fun observe(locationKey: String): Flow<WeatherCacheEntity?> =
        rows.map { it[locationKey] }.distinctUntilChanged()

    override suspend fun get(locationKey: String): WeatherCacheEntity? = rows.value[locationKey]

    override suspend fun upsert(entity: WeatherCacheEntity) {
        rows.update { it + (entity.locationKey to entity) }
    }

    override suspend fun delete(locationKey: String) {
        rows.update { it - locationKey }
    }
}

class FakePreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        transform(state.value).also { state.value = it }
}

/** In-memory [SavedCityDao]; mirrors the unique (latitude, longitude) index and id auto-increment. */
class FakeSavedCityDao : SavedCityDao() {
    val rows = MutableStateFlow<List<SavedCityEntity>>(emptyList())
    val deletedWeatherKeys = mutableListOf<String>()
    private var nextId = 1L

    private fun sorted() = rows.value.sortedWith(compareBy({ it.position }, { it.id }))

    override fun observeAll(): Flow<List<SavedCityEntity>> = rows.map { sorted() }

    override suspend fun getAll(): List<SavedCityEntity> = sorted()

    override suspend fun get(id: Long): SavedCityEntity? = rows.value.find { it.id == id }

    override suspend fun findByCoordinates(latitude: Double, longitude: Double): SavedCityEntity? =
        rows.value.find { it.latitude == latitude && it.longitude == longitude }

    override suspend fun maxPosition(): Int? = rows.value.maxOfOrNull { it.position }

    override suspend fun insert(entity: SavedCityEntity): Long {
        if (findByCoordinates(entity.latitude, entity.longitude) != null) return -1
        if (entity.id != 0L && get(entity.id) != null) return -1
        val id = if (entity.id == 0L) nextId else entity.id
        nextId = maxOf(nextId, id + 1)
        rows.update { it + entity.copy(id = id) }
        return id
    }

    override suspend fun deleteCity(id: Long): Int {
        val before = rows.value.size
        rows.update { cities -> cities.filterNot { it.id == id } }
        return before - rows.value.size
    }

    override suspend fun deleteCachedWeather(cacheKey: String) {
        deletedWeatherKeys += cacheKey
    }
}
