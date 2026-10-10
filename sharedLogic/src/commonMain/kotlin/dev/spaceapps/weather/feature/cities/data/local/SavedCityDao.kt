package dev.spaceapps.weather.feature.cities.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SavedCityDao {

    @Query("SELECT * FROM saved_city ORDER BY position, id")
    abstract fun observeAll(): Flow<List<SavedCityEntity>>

    @Query("SELECT * FROM saved_city ORDER BY position, id")
    abstract suspend fun getAll(): List<SavedCityEntity>

    @Query("SELECT * FROM saved_city WHERE id = :id")
    abstract suspend fun get(id: Long): SavedCityEntity?

    @Query("SELECT * FROM saved_city WHERE latitude = :latitude AND longitude = :longitude")
    abstract suspend fun findByCoordinates(latitude: Double, longitude: Double): SavedCityEntity?

    @Query("SELECT MAX(position) FROM saved_city")
    abstract suspend fun maxPosition(): Int?

    /** Returns the new row id, or -1 when a city with the same coordinates already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(entity: SavedCityEntity): Long

    @Query("DELETE FROM saved_city WHERE id = :id")
    abstract suspend fun deleteCity(id: Long): Int

    @Query("DELETE FROM weather_cache WHERE locationKey = :cacheKey")
    abstract suspend fun deleteCachedWeather(cacheKey: String)

    /** Appends [entity] after the last city. Returns the row id, or -1 if it is already saved. */
    @Transaction
    open suspend fun insertLast(entity: SavedCityEntity): Long {
        val position = (maxPosition() ?: -1) + 1
        return insert(entity.copy(position = position))
    }

    /** Deletes the city together with its cached weather. Returns false if it did not exist. */
    @Transaction
    open suspend fun deleteWithWeather(id: Long, cacheKey: String): Boolean {
        val deleted = deleteCity(id) > 0
        deleteCachedWeather(cacheKey)
        return deleted
    }
}
