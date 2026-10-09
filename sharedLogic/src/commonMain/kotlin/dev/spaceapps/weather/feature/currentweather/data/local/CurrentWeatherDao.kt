package dev.spaceapps.weather.feature.currentweather.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrentWeatherDao {

    @Query("SELECT * FROM current_weather WHERE id = ${CurrentWeatherEntity.CURRENT_LOCATION_ID}")
    fun observe(): Flow<CurrentWeatherEntity?>

    @Query("SELECT * FROM current_weather WHERE id = ${CurrentWeatherEntity.CURRENT_LOCATION_ID}")
    suspend fun get(): CurrentWeatherEntity?

    @Upsert
    suspend fun upsert(entity: CurrentWeatherEntity)
}
