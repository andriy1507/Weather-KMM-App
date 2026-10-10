package dev.spaceapps.weather.core.database

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.DeleteTable
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.migration.AutoMigrationSpec
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.spaceapps.weather.feature.cities.data.local.SavedCityDao
import dev.spaceapps.weather.feature.cities.data.local.SavedCityEntity
import dev.spaceapps.weather.feature.weather.data.local.WeatherCacheDao
import dev.spaceapps.weather.feature.weather.data.local.WeatherCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

internal const val DATABASE_FILE_NAME = "weather.db"

/**
 * Schema history (exported to `sharedLogic/schemas`, keep those files in git):
 * - 1: `current_weather` single-row cache.
 * - 2: `weather_cache` keyed by location, `saved_city`.
 *
 * Saved cities are user data: every version bump needs a migration (prefer [AutoMigration]).
 */
@Database(
    entities = [WeatherCacheEntity::class, SavedCityEntity::class],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = AppDatabase.Migration1To2::class),
    ],
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun savedCityDao(): SavedCityDao

    /** The v1 cache is simply dropped; the weather is fetched again on next start. */
    @DeleteTable(tableName = "current_weather")
    class Migration1To2 : AutoMigrationSpec
}

// The Room compiler generates the `actual` implementations.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

fun createAppDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        // Downgrades only happen with development builds; upgrades must always migrate.
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
        .build()
