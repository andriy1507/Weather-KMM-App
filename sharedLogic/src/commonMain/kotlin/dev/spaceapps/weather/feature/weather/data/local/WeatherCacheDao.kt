package dev.spaceapps.weather.feature.weather.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherCacheDao {

    @Query("SELECT * FROM weather_cache WHERE locationKey = :locationKey")
    fun observe(locationKey: String): Flow<WeatherCacheEntity?>

    @Query("SELECT * FROM weather_cache WHERE locationKey = :locationKey")
    suspend fun get(locationKey: String): WeatherCacheEntity?

    @Upsert
    suspend fun upsert(entity: WeatherCacheEntity)

    @Query("DELETE FROM weather_cache WHERE locationKey = :locationKey")
    suspend fun delete(locationKey: String)
}
